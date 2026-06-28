package com.hotel.room.service;

import com.hotel.common.core.BusinessException;
import com.hotel.room.model.entity.Room;
import com.hotel.room.model.enums.RoomStatus;
import com.hotel.room.model.event.RoomStatusChangeEvent;
import com.hotel.room.repository.RoomMapper;
import com.hotel.room.service.strategy.AssignmentStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomAssignmentService {

    private final RoomMapper roomMapper;
    private final RoomInventoryService inventoryService;
    private final RedissonClient redisson;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Qualifier("sequentialStrategy")
    private final AssignmentStrategy defaultStrategy;

    @Value("${room.assignment.lock-ttl-seconds:30}")
    private int lockTtlSeconds;

    private static final String LOCK_PREFIX = "room:lock:";

    /**
     * 排房：扣减库存 → 选房 → 锁定房间（Redisson 分布式锁）。
     *
     * @return 分配结果
     */
    @Transactional
    public RoomAssignmentResult assign(String orderId, String hotelId, String roomTypeId) {
        if (!inventoryService.decrement(hotelId, roomTypeId, 1)) {
            return RoomAssignmentResult.fail("房间库存不足");
        }

        try {
            List<Room> available = roomMapper.selectAvailable(hotelId, roomTypeId);
            if (available.isEmpty()) {
                inventoryService.increment(hotelId, roomTypeId, 1);
                return RoomAssignmentResult.fail("无可用房间");
            }

            Room selected = defaultStrategy.select(available);
            Room locked = lockAndAssign(selected, orderId);
            if (locked == null) {
                inventoryService.increment(hotelId, roomTypeId, 1);
                return RoomAssignmentResult.fail("房间锁定失败，已被其他操作占用");
            }

            kafkaTemplate.send("room-status-changes", RoomStatusChangeEvent.builder()
                    .hotelId(hotelId)
                    .roomTypeId(roomTypeId)
                    .roomId(locked.getRoomId())
                    .roomNo(locked.getRoomNo())
                    .orderId(orderId)
                    .changeType("RESERVED")
                    .timestamp(System.currentTimeMillis())
                    .build());

            return RoomAssignmentResult.success(locked);
        } catch (Exception e) {
            inventoryService.increment(hotelId, roomTypeId, 1);
            throw e;
        }
    }

    /**
     * 使用指定策略排房。
     */
    @Transactional
    public RoomAssignmentResult assignWithStrategy(String orderId, String hotelId, String roomTypeId,
                                                    AssignmentStrategy strategy) {
        if (!inventoryService.decrement(hotelId, roomTypeId, 1)) {
            return RoomAssignmentResult.fail("房间库存不足");
        }

        try {
            List<Room> available = roomMapper.selectAvailable(hotelId, roomTypeId);
            if (available.isEmpty()) {
                inventoryService.increment(hotelId, roomTypeId, 1);
                return RoomAssignmentResult.fail("无可用房间");
            }

            Room selected = strategy.select(available);
            Room locked = lockAndAssign(selected, orderId);
            if (locked == null) {
                inventoryService.increment(hotelId, roomTypeId, 1);
                return RoomAssignmentResult.fail("房间锁定失败");
            }

            return RoomAssignmentResult.success(locked);
        } catch (Exception e) {
            inventoryService.increment(hotelId, roomTypeId, 1);
            throw e;
        }
    }

    /**
     * 释放房间（Saga 补偿 / 退房）。
     */
    @Transactional
    public void release(String roomId, String orderId) {
        Room room = roomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException("房间不存在");
        }

        int updated = roomMapper.updateStatusWithCheck(roomId,
                RoomStatus.VACANT.getCode(), RoomStatus.RESERVED.getCode());
        if (updated == 0) {
            log.warn("释放房间失败，状态不符: roomId={}, currentStatus={}", roomId, room.getRoomStatus());
            return;
        }

        inventoryService.increment(room.getHotelId(), room.getRoomTypeId(), 1);
        kafkaTemplate.send("room-status-changes", RoomStatusChangeEvent.builder()
                .hotelId(room.getHotelId())
                .roomTypeId(room.getRoomTypeId())
                .roomId(roomId)
                .roomNo(room.getRoomNo())
                .orderId(orderId)
                .changeType("RELEASED")
                .timestamp(System.currentTimeMillis())
                .build());

        log.info("房间已释放: roomId={}, orderId={}", roomId, orderId);
    }

    private Room lockAndAssign(Room room, String orderId) {
        String lockKey = LOCK_PREFIX + room.getRoomId();
        RLock lock = redisson.getLock(lockKey);

        try {
            if (!lock.tryLock(0, lockTtlSeconds, TimeUnit.SECONDS)) {
                log.warn("获取房间锁失败: roomId={}, orderId={}", room.getRoomId(), orderId);
                return null;
            }

            int updated = roomMapper.updateStatusWithCheck(room.getRoomId(),
                    RoomStatus.RESERVED.getCode(), RoomStatus.VACANT.getCode());
            if (updated == 0) {
                log.warn("房间状态已变更，锁定失败: roomId={}, currentStatus={}", room.getRoomId(), room.getRoomStatus());
                return null;
            }

            room.setRoomStatus(RoomStatus.RESERVED.getCode());
            log.info("房间锁定成功: roomId={}, roomNo={}, orderId={}", room.getRoomId(), room.getRoomNo(), orderId);
            return room;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public record RoomAssignmentResult(boolean success, String message, Room room) {
        public static RoomAssignmentResult success(Room room) {
            return new RoomAssignmentResult(true, "排房成功", room);
        }

        public static RoomAssignmentResult fail(String message) {
            return new RoomAssignmentResult(false, message, null);
        }
    }
}
