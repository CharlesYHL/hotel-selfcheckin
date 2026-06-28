package com.hotel.room.service;

import com.hotel.room.model.entity.RoomType;
import com.hotel.room.repository.RoomMapper;
import com.hotel.room.repository.RoomTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventorySyncService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RoomMapper roomMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final RoomInventoryService inventoryService;

    private static final String KEY_PREFIX = "room:type:inventory:";

    /**
     * 定时对账：每 5 分钟用 MySQL 库存校准 Redis。
     */
    @Scheduled(fixedRateString = "${inventory.sync-interval-minutes:5}000", initialDelay = 60000)
    public void reconcile() {
        try {
            List<RoomType> types = roomTypeMapper.selectList(null);
            int synced = 0, drift = 0;

            for (RoomType t : types) {
                int dbAvailable = roomMapper.countAvailable(t.getHotelId(), t.getRoomTypeId());
                int redisAvailable = inventoryService.getInventory(t.getHotelId(), t.getRoomTypeId());

                if (dbAvailable != redisAvailable) {
                    log.warn("库存漂移检测: hotelId={}, roomTypeId={}, redis={}, mysql={}",
                            t.getHotelId(), t.getRoomTypeId(), redisAvailable, dbAvailable);
                    redisTemplate.opsForHash().put(KEY_PREFIX + t.getHotelId(), t.getRoomTypeId(), String.valueOf(dbAvailable));
                    drift++;
                }
                synced++;
            }
            log.info("库存对账完成: 同步 {} 种房型, 修复 {} 处漂移", synced, drift);
        } catch (Exception e) {
            log.error("库存对账失败", e);
        }
    }

    /**
     * 从 MySQL 全量重载指定酒店的库存到 Redis。
     */
    public void reload(String hotelId) {
        List<Map<String, Object>> rows = roomMapper.countAvailableGroupByType(hotelId);
        for (Map<String, Object> row : rows) {
            String typeId = row.get("room_type_id").toString();
            int count = Integer.parseInt(row.get("cnt").toString());
            redisTemplate.opsForHash().put(KEY_PREFIX + hotelId, typeId, String.valueOf(count));
        }
        log.info("库存重载完成: hotelId={}, 房型数={}", hotelId, rows.size());
    }
}
