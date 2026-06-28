package com.hotel.common.cache;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@Slf4j
public class CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redisson;

    private static final String LOCK_PREFIX = "lock:";

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            return (T) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            return null;
        }
    }

    public <T> void set(String key, T val, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, val, ttl);
        } catch (Exception e) {
            log.error("cache set 失败: {}", key, e);
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception ignored) {
        }
    }

    // ===== 逻辑过期（防击穿）=====

    public <T> void setLogical(String key, T val, Duration logicalTTL) {
        LogicalEntry<T> entry = LogicalEntry.<T>builder()
                .value(val)
                .logicalExpireTime(System.currentTimeMillis() + logicalTTL.toMillis())
                .build();
        set(key, entry, logicalTTL.multipliedBy(3));
    }

    @SuppressWarnings("unchecked")
    public <T> LogicalResult<T> getLogical(String key) {
        LogicalEntry<T> e = get(key);
        if (e == null) return LogicalResult.miss();
        if (e.isLogicallyExpired()) return LogicalResult.expired(e.getValue());
        return LogicalResult.hit(e.getValue());
    }

    // ===== 防击穿（互斥锁 + 逻辑过期）=====

    public <T> T getWithBreakdownProtection(String key, Class<T> clazz,
                                             Supplier<T> loader, Duration logicalTTL) {
        LogicalResult<T> r = getLogical(key);
        if (r.isHit() && !r.isLogicallyExpired()) return r.getValue();

        RLock lock = redisson.getLock(LOCK_PREFIX + key);
        try {
            if (lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                try {
                    LogicalResult<T> dc = getLogical(key);
                    if (dc.isHit() && !dc.isLogicallyExpired()) return dc.getValue();
                    T val = loader.get();
                    if (val != null) {
                        setLogical(key, val, logicalTTL);
                    }
                    return val;
                } finally {
                    if (lock.isHeldByCurrentThread()) lock.unlock();
                }
            } else {
                Thread.sleep(50);
                LogicalResult<T> retry = getLogical(key);
                return retry.isHit() ? retry.getValue() : loader.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return loader.get();
        } catch (Exception e) {
            log.error("防击穿异常: {}", key, e);
            return loader.get();
        }
    }

    // ===== 防雪崩（随机过期偏移）=====

    public <T> void setWithJitter(String key, T val, Duration base, Duration jitter) {
        long jitterMs = (long) (Math.random() * jitter.toMillis());
        set(key, val, base.plus(Duration.ofMillis(jitterMs)));
    }

    // ===== 防穿透（布隆过滤器 + 空值缓存）=====

    public void initBloom(String name, long expectedInsertions) {
        RBloomFilter<Object> filter = redisson.getBloomFilter(name);
        filter.tryInit(expectedInsertions, 0.01);
    }

    public void addToBloom(String name, Object val) {
        redisson.getBloomFilter(name).add(val);
    }

    public boolean mightExistBloom(String name, Object val) {
        return redisson.getBloomFilter(name).contains(val);
    }

    public <T> T getWithNullProtection(String key, Class<T> clazz, Supplier<T> loader, Duration nullTTL) {
        Object cached = get(key);
        if (cached != null) {
            if (cached instanceof NullValue) return null;
            return (T) cached;
        }
        T val = loader.get();
        if (val == null) {
            set(key, NullValue.INSTANCE, nullTTL);
        } else {
            set(key, val, Duration.ofHours(1));
        }
        return val;
    }

    @Data
    @Builder
    public static class LogicalEntry<T> implements Serializable {
        private T value;
        private long logicalExpireTime;
        public boolean isLogicallyExpired() {
            return System.currentTimeMillis() > logicalExpireTime;
        }
    }

    public static class LogicalResult<T> {
        private boolean hit;
        private boolean expired;
        private T val;

        public static <T> LogicalResult<T> hit(T v) {
            LogicalResult<T> r = new LogicalResult<>();
            r.hit = true;
            r.val = v;
            return r;
        }

        public static <T> LogicalResult<T> expired(T v) {
            LogicalResult<T> r = new LogicalResult<>();
            r.hit = true;
            r.expired = true;
            r.val = v;
            return r;
        }

        public static <T> LogicalResult<T> miss() {
            return new LogicalResult<>();
        }

        public boolean isHit() { return hit; }
        public boolean isLogicallyExpired() { return expired; }
        public T getValue() { return val; }
    }

    public static class NullValue implements Serializable {
        public static final NullValue INSTANCE = new NullValue();
    }
}
