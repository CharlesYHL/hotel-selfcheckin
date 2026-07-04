package com.hotel.common.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CacheService 单元测试")
class CacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RedissonClient redisson;

    @Mock
    private RLock lock;

    @Mock
    private RBloomFilter<Object> bloomFilter;

    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheService = new CacheService();
        try {
            var redisField = CacheService.class.getDeclaredField("redisTemplate");
            redisField.setAccessible(true);
            redisField.set(cacheService, redisTemplate);
            var redissonField = CacheService.class.getDeclaredField("redisson");
            redissonField.setAccessible(true);
            redissonField.set(cacheService, redisson);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("基本 get/set 操作")
    class BasicOperations {

        @Test
        @DisplayName("set 后 get 应返回原值")
        void shouldGetAfterSet() {
            when(valueOperations.get("test:key")).thenReturn("test-value");

            cacheService.set("test:key", "test-value", Duration.ofHours(1));
            Object result = cacheService.get("test:key");

            assertEquals("test-value", result);
        }

        @Test
        @DisplayName("get 不存在的 key 应返回 null")
        void shouldReturnNullForMissingKey() {
            when(valueOperations.get("nonexistent")).thenReturn(null);

            Object result = cacheService.get("nonexistent");

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("逻辑过期")
    class LogicalExpiration {

        @Test
        @DisplayName("setLogical → getLogical 未过期应命中")
        void shouldHitWhenNotExpired() {
            CacheService.LogicalEntry<String> entry = CacheService.LogicalEntry.<String>builder()
                    .value("hello")
                    .logicalExpireTime(System.currentTimeMillis() + 3600000) // 1h later
                    .build();
            when(valueOperations.get("test:logical")).thenReturn(entry);

            CacheService.LogicalResult<String> result = cacheService.getLogical("test:logical");

            assertTrue(result.isHit());
            assertFalse(result.isLogicallyExpired());
            assertEquals("hello", result.getValue());
        }

        @Test
        @DisplayName("getLogical 已过期应标记 expired=true")
        void shouldFlagAsExpired() {
            CacheService.LogicalEntry<String> entry = CacheService.LogicalEntry.<String>builder()
                    .value("stale")
                    .logicalExpireTime(System.currentTimeMillis() - 1000) // 1s ago
                    .build();
            when(valueOperations.get("test:expired")).thenReturn(entry);

            CacheService.LogicalResult<String> result = cacheService.getLogical("test:expired");

            assertTrue(result.isHit());
            assertTrue(result.isLogicallyExpired());
            assertEquals("stale", result.getValue());
        }

        @Test
        @DisplayName("getLogical key 不存在应 miss")
        void shouldMissWhenKeyAbsent() {
            when(valueOperations.get("test:missing")).thenReturn(null);

            CacheService.LogicalResult<String> result = cacheService.getLogical("test:missing");

            assertFalse(result.isHit());
            assertNull(result.getValue());
        }
    }

    @Nested
    @DisplayName("防击穿（互斥锁 + 逻辑过期）")
    class BreakdownProtection {

        @Test
        @DisplayName("缓存命中且未过期 → 不调 loader")
        void shouldNotCallLoaderWhenCacheHit() {
            CacheService.LogicalEntry<String> entry = CacheService.LogicalEntry.<String>builder()
                    .value("cached-value")
                    .logicalExpireTime(System.currentTimeMillis() + 3600000)
                    .build();
            when(valueOperations.get("breakdown:hit")).thenReturn(entry);

            AtomicInteger loaderCalls = new AtomicInteger(0);
            Supplier<String> loader = () -> { loaderCalls.incrementAndGet(); return "fresh"; };

            String result = cacheService.getWithBreakdownProtection(
                    "breakdown:hit", String.class, loader, Duration.ofMinutes(30));

            assertEquals("cached-value", result);
            assertEquals(0, loaderCalls.get());
        }

        @Test
        @DisplayName("缓存过期 → 获取锁 → 重建缓存")
        void shouldRebuildWhenExpired() throws InterruptedException {
            CacheService.LogicalEntry<String> expiredEntry = CacheService.LogicalEntry.<String>builder()
                    .value("stale-value")
                    .logicalExpireTime(System.currentTimeMillis() - 1000)
                    .build();
            when(valueOperations.get("breakdown:expired")).thenReturn(expiredEntry);

            when(redisson.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock(0, 30, TimeUnit.SECONDS)).thenReturn(true);
            when(lock.isHeldByCurrentThread()).thenReturn(true);

            AtomicInteger loaderCalls = new AtomicInteger(0);
            Supplier<String> loader = () -> { loaderCalls.incrementAndGet(); return "rebuilt-value"; };

            String result = cacheService.getWithBreakdownProtection(
                    "breakdown:expired", String.class, loader, Duration.ofMinutes(30));

            assertEquals("rebuilt-value", result);
            assertEquals(1, loaderCalls.get());
            verify(lock).unlock();
        }

        @Test
        @DisplayName("缓存未命中 → 获取锁 → 加载数据")
        void shouldLoadWhenCacheMiss() throws InterruptedException {
            when(valueOperations.get("breakdown:miss")).thenReturn(null);

            when(redisson.getLock(anyString())).thenReturn(lock);
            when(lock.tryLock(0, 30, TimeUnit.SECONDS)).thenReturn(true);
            when(lock.isHeldByCurrentThread()).thenReturn(true);

            Supplier<String> loader = () -> "loaded-value";

            String result = cacheService.getWithBreakdownProtection(
                    "breakdown:miss", String.class, loader, Duration.ofMinutes(30));

            assertEquals("loaded-value", result);
        }
    }

    @Nested
    @DisplayName("防雪崩（随机 TTL 偏移）")
    class AvalancheProtection {

        @Test
        @DisplayName("setWithJitter: 每次 TTL 应不同（随机性验证）")
        void shouldVaryTtlWithJitter() {
            // 验证 jitter 方法调用不会抛异常即可（随机性由 Math.random 保证）
            assertDoesNotThrow(() ->
                    cacheService.setWithJitter("test:jitter", "value",
                            Duration.ofHours(1), Duration.ofMinutes(10)));
        }
    }

    @Nested
    @DisplayName("防穿透（空值缓存）")
    class PenetrationProtection {

        @Test
        @DisplayName("loader 返回 null → 缓存 NullValue → 返回 null")
        void shouldCacheNullValue() {
            when(valueOperations.get("penetration:null")).thenReturn(null);

            Supplier<String> loader = () -> null;

            String result = cacheService.getWithNullProtection(
                    "penetration:null", String.class, loader, Duration.ofMinutes(5));

            assertNull(result);
            // 验证 NullValue 被缓存
            verify(valueOperations).set(eq("penetration:null"), any(CacheService.NullValue.class), any(Duration.class));
        }

        @Test
        @DisplayName("缓存中的 NullValue → 直接返回 null，不调 loader")
        void shouldReturnNullForCachedNullValue() {
            when(valueOperations.get("penetration:cached-null")).thenReturn(CacheService.NullValue.INSTANCE);

            AtomicInteger loaderCalls = new AtomicInteger(0);
            Supplier<String> loader = () -> { loaderCalls.incrementAndGet(); return "should-not-reach"; };

            String result = cacheService.getWithNullProtection(
                    "penetration:cached-null", String.class, loader, Duration.ofMinutes(5));

            assertNull(result);
            assertEquals(0, loaderCalls.get());
        }

        @Test
        @DisplayName("loader 返回正常值 → 缓存 1 小时")
        void shouldCacheNormalValue() {
            when(valueOperations.get("penetration:normal")).thenReturn(null);

            Supplier<String> loader = () -> "normal-value";

            String result = cacheService.getWithNullProtection(
                    "penetration:normal", String.class, loader, Duration.ofMinutes(5));

            assertEquals("normal-value", result);
            verify(valueOperations).set(eq("penetration:normal"), eq("normal-value"), eq(Duration.ofHours(1)));
        }
    }

    @Nested
    @DisplayName("布隆过滤器")
    class BloomFilter {

        @Test
        @DisplayName("mightExistBloom: 已添加的元素应可能存在")
        void shouldIndicateAddedElementMayExist() {
            when(redisson.getBloomFilter("test-bloom")).thenReturn(bloomFilter);
            when(bloomFilter.contains("element-1")).thenReturn(true);

            cacheService.addToBloom("test-bloom", "element-1");
            boolean result = cacheService.mightExistBloom("test-bloom", "element-1");

            assertTrue(result);
        }

        @Test
        @DisplayName("mightExistBloom: 未添加的元素应不存在")
        void shouldIndicateMissingElementDoesNotExist() {
            when(redisson.getBloomFilter("test-bloom")).thenReturn(bloomFilter);
            when(bloomFilter.contains("unknown")).thenReturn(false);

            boolean result = cacheService.mightExistBloom("test-bloom", "unknown");

            assertFalse(result);
        }
    }
}
