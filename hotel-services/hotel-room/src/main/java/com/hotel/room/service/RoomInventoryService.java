package com.hotel.room.service;

import com.hotel.common.core.BusinessException;
import com.hotel.room.model.entity.RoomType;
import com.hotel.room.repository.RoomMapper;
import com.hotel.room.repository.RoomTypeMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomInventoryService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RoomTypeMapper roomTypeMapper;
    private final RoomMapper roomMapper;

    private static final String KEY_PREFIX = "room:type:inventory:";

    private static final String DECREMENT_SCRIPT =
            "local cur = redis.call('HGET', KEYS[1], ARGV[1]) " +
                    "if not cur then return -2 " +
                    "elseif tonumber(cur) < tonumber(ARGV[2]) then return -1 " +
                    "else return redis.call('HINCRBY', KEYS[1], ARGV[1], -tonumber(ARGV[2])) end";

    private static final String BATCH_DECREMENT_SCRIPT =
            "local inv = {} local ok = true " +
                    "for i = 1, #KEYS do " +
                    "  local c = redis.call('HGET', KEYS[i], ARGV[1]) " +
                    "  if not c or tonumber(c) < tonumber(ARGV[2]) then ok = false break end " +
                    "  inv[i] = tonumber(c) " +
                    "end " +
                    "if ok then for i = 1, #KEYS do redis.call('HINCRBY', KEYS[i], ARGV[1], -tonumber(ARGV[2])) end return 1 " +
                    "else return 0 end";

    private DefaultRedisScript<Long> decrementScript;
    private DefaultRedisScript<Long> batchScript;

    {
        decrementScript = new DefaultRedisScript<>();
        decrementScript.setScriptText(DECREMENT_SCRIPT);
        decrementScript.setResultType(Long.class);
        batchScript = new DefaultRedisScript<>();
        batchScript.setScriptText(BATCH_DECREMENT_SCRIPT);
        batchScript.setResultType(Long.class);
    }

    /**
     * 扣减库存，原子操作，永不超卖。
     *
     * @return true=扣减成功, false=库存不足或不存在
     */
    public boolean decrement(String hotelId, String roomTypeId, int count) {
        if (count <= 0) throw new IllegalArgumentException("count must > 0");
        String key = KEY_PREFIX + hotelId;
        Long r = redisTemplate.execute(decrementScript, List.of(key), roomTypeId, String.valueOf(count));
        if (r == null || r < 0) {
            log.warn("库存扣减失败: hotelId={}, roomTypeId={}, count={}, result={}", hotelId, roomTypeId, count, r);
            return false;
        }
        log.info("库存扣减成功: hotelId={}, roomTypeId={}, count={}, remaining={}", hotelId, roomTypeId, count, r);
        return true;
    }

    /**
     * 释放库存（退房/取消订单/排房失败回滚）。
     */
    public long increment(String hotelId, String roomTypeId, int count) {
        String key = KEY_PREFIX + hotelId;
        Long r = redisTemplate.opsForHash().increment(key, roomTypeId, count);
        log.info("库存释放: hotelId={}, roomTypeId={}, count={}, newCount={}", hotelId, roomTypeId, count, r);
        return r != null ? r : 0;
    }

    public int getInventory(String hotelId, String roomTypeId) {
        Object v = redisTemplate.opsForHash().get(KEY_PREFIX + hotelId, roomTypeId);
        return v != null ? Integer.parseInt(v.toString()) : 0;
    }

    public Map<String, Integer> getAll(String hotelId) {
        Map<Object, Object> m = redisTemplate.opsForHash().entries(KEY_PREFIX + hotelId);
        return m.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(), e -> Integer.parseInt(e.getValue().toString())));
    }

    /**
     * 批量扣减，全部成功或全部失败。
     */
    public boolean decrementBatch(String hotelId, List<String> roomTypeIds, int countPerType) {
        if (roomTypeIds == null || roomTypeIds.isEmpty()) return true;
        List<String> keys = roomTypeIds.stream().map(id -> KEY_PREFIX + hotelId).toList();
        Long r = redisTemplate.execute(batchScript, keys, roomTypeIds.get(0), String.valueOf(countPerType));
        return r != null && r == 1;
    }

    /**
     * 启动时从 MySQL 加载库存到 Redis。
     */
    @PostConstruct
    public void warmup() {
        try {
            List<RoomType> types = roomTypeMapper.selectList(null);
            for (RoomType t : types) {
                int available = roomMapper.countAvailable(t.getHotelId(), t.getRoomTypeId());
                if (available > 0) {
                    redisTemplate.opsForHash().put(KEY_PREFIX + t.getHotelId(), t.getRoomTypeId(), String.valueOf(available));
                }
            }
            log.info("库存预热完成, 加载 {} 种房型", types.size());
        } catch (Exception e) {
            log.warn("库存预热失败（数据库可能未就绪）: {}", e.getMessage());
        }
    }

    /**
     * 从 MySQL 刷新指定酒店指定房型的库存。
     */
    public void refreshFromDb(String hotelId, String roomTypeId) {
        int available = roomMapper.countAvailable(hotelId, roomTypeId);
        redisTemplate.opsForHash().put(KEY_PREFIX + hotelId, roomTypeId, String.valueOf(available));
        log.info("库存刷新: hotelId={}, roomTypeId={}, available={}", hotelId, roomTypeId, available);
    }

    /**
     * 检查库存是否低于告警阈值。
     */
    public boolean isLowInventory(String hotelId, String roomTypeId, int thresholdPercent) {
        RoomType type = roomTypeMapper.selectById(roomTypeId);
        if (type == null) return false;
        int current = getInventory(hotelId, roomTypeId);
        int max = type.getMaxRooms();
        if (max == 0) return false;
        return current * 100 / max < thresholdPercent;
    }
}
