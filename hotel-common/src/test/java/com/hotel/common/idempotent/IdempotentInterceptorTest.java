package com.hotel.common.idempotent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("IdempotentInterceptor 单元测试")
class IdempotentInterceptorTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private IdempotentInterceptor interceptor;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    /**
     * Helper to create a mock Idempotent annotation.
     * We use a simple stub since we can't instantiate annotations directly.
     */
    private Idempotent mockIdempotent(String keyPrefix, String[] fields, long expireSeconds,
                                       long timeoutSeconds, boolean deleteOnSuccess,
                                       Idempotent.ConcurrentStrategy strategy) {
        return new Idempotent() {
            @Override
            public String keyPrefix() { return keyPrefix; }
            @Override
            public String staticKey() { return ""; }
            @Override
            public String[] fields() { return fields; }
            @Override
            public long expireSeconds() { return expireSeconds; }
            @Override
            public long timeoutSeconds() { return timeoutSeconds; }
            @Override
            public boolean deleteOnSuccess() { return deleteOnSuccess; }
            @Override
            public ConcurrentStrategy concurrentStrategy() { return strategy; }
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return Idempotent.class; }
        };
    }

    /**
     * Helper to create a mock Idempotent annotation with staticKey support.
     */
    private Idempotent mockIdempotentWithStaticKey(String keyPrefix, String staticKey, String[] fields,
                                                    long expireSeconds, long timeoutSeconds,
                                                    boolean deleteOnSuccess,
                                                    Idempotent.ConcurrentStrategy strategy) {
        return new Idempotent() {
            @Override
            public String keyPrefix() { return keyPrefix; }
            @Override
            public String staticKey() { return staticKey; }
            @Override
            public String[] fields() { return fields; }
            @Override
            public long expireSeconds() { return expireSeconds; }
            @Override
            public long timeoutSeconds() { return timeoutSeconds; }
            @Override
            public boolean deleteOnSuccess() { return deleteOnSuccess; }
            @Override
            public ConcurrentStrategy concurrentStrategy() { return strategy; }
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return Idempotent.class; }
        };
    }

    @Nested
    @DisplayName("首次请求")
    class FirstRequest {

        @Test
        @DisplayName("SETNX 成功 → 执行业务 → 标记 SUCCESS → 缓存结果")
        void shouldExecuteAndCacheOnFirstRequest() throws Throwable {
            Idempotent annotation = mockIdempotent("test:first", new String[0],
                    3600, 0, false, Idempotent.ConcurrentStrategy.WAIT);

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getName()).thenReturn("testMethod");
            when(joinPoint.getTarget()).thenReturn(new Object());
            when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class)))
                    .thenReturn(true);
            when(joinPoint.proceed()).thenReturn("success-result");
            doNothing().when(valueOperations).set(anyString(), eq("SUCCESS"), any(Duration.class));

            Object result = interceptor.around(joinPoint, annotation);

            assertEquals("success-result", result);
            verify(joinPoint).proceed();
            // 验证结果被缓存
            verify(valueOperations).set(startsWith("idempotent:result:"), eq("\"success-result\""), any(Duration.class));
        }

        @Test
        @DisplayName("deleteOnSuccess=true → 执行后删除 key")
        void shouldDeleteKeyOnSuccess() throws Throwable {
            Idempotent annotation = mockIdempotent("test:delete", new String[0],
                    3600, 0, true, Idempotent.ConcurrentStrategy.WAIT);

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getName()).thenReturn("testMethod");
            when(joinPoint.getTarget()).thenReturn(new Object());
            when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class)))
                    .thenReturn(true);
            when(joinPoint.proceed()).thenReturn("result");
            when(redisTemplate.delete(anyString())).thenReturn(true);

            Object result = interceptor.around(joinPoint, annotation);

            assertEquals("result", result);
            verify(redisTemplate).delete(startsWith("idempotent:"));
        }
    }

    @Nested
    @DisplayName("重复请求 — SUCCESS 状态")
    class DuplicateRequestSuccess {

        @Test
        @DisplayName("已 SUCCESS → 返回缓存结果，不执行业务")
        void shouldReturnCachedResultOnDuplicate() throws Throwable {
            Idempotent annotation = mockIdempotent("test:dup", new String[0],
                    3600, 0, false, Idempotent.ConcurrentStrategy.WAIT);

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getName()).thenReturn("testMethod");
            when(joinPoint.getTarget()).thenReturn(new Object());
            when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class)))
                    .thenReturn(false);
            when(valueOperations.get(startsWith("idempotent:")))
                    .thenReturn("SUCCESS");
            when(valueOperations.get(startsWith("idempotent:result:")))
                    .thenReturn("\"cached-value\"");

            Object result = interceptor.around(joinPoint, annotation);

            // 由于 ObjectMapper 反序列化返回 String，检查不为 null
            assertNotNull(result);
            verify(joinPoint, never()).proceed();
        }
    }

    @Nested
    @DisplayName("并发策略")
    class ConcurrencyStrategy {

        @Test
        @DisplayName("THROW 策略 → 直接抛 IdempotentException")
        void shouldThrowOnConcurrentWithThrowStrategy() throws Throwable {
            Idempotent annotation = mockIdempotent("test:throw", new String[0],
                    3600, 0, false, Idempotent.ConcurrentStrategy.THROW);

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getName()).thenReturn("testMethod");
            when(joinPoint.getTarget()).thenReturn(new Object());
            when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class)))
                    .thenReturn(false);
            when(valueOperations.get(startsWith("idempotent:")))
                    .thenReturn("PROCESSING");

            assertThrows(IdempotentException.class, () -> interceptor.around(joinPoint, annotation));
        }

        @Test
        @DisplayName("SKIP 策略 → 返回 null")
        void shouldReturnNullWithSkipStrategy() throws Throwable {
            Idempotent annotation = mockIdempotent("test:skip", new String[0],
                    3600, 0, false, Idempotent.ConcurrentStrategy.SKIP);

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getName()).thenReturn("testMethod");
            when(joinPoint.getTarget()).thenReturn(new Object());
            when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class)))
                    .thenReturn(false);
            when(valueOperations.get(startsWith("idempotent:")))
                    .thenReturn("PROCESSING");

            Object result = interceptor.around(joinPoint, annotation);

            assertNull(result);
        }

        @Test
        @DisplayName("WAIT 策略超时 → IdempotentException")
        void shouldThrowOnWaitTimeout() throws Throwable {
            Idempotent annotation = mockIdempotent("test:wait", new String[0],
                    3600, 1, false, Idempotent.ConcurrentStrategy.WAIT);

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getName()).thenReturn("testMethod");
            when(joinPoint.getTarget()).thenReturn(new Object());
            when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class)))
                    .thenReturn(false);
            // 始终返回 PROCESSING，永不变成 SUCCESS
            when(valueOperations.get(startsWith("idempotent:")))
                    .thenReturn("PROCESSING");

            assertThrows(IdempotentException.class, () -> interceptor.around(joinPoint, annotation));
        }
    }

    @Nested
    @DisplayName("异常处理")
    class ExceptionHandling {

        @Test
        @DisplayName("业务抛异常 → 删除幂等 key + 结果缓存")
        void shouldCleanUpOnBusinessException() throws Throwable {
            Idempotent annotation = mockIdempotent("test:error", new String[0],
                    3600, 0, false, Idempotent.ConcurrentStrategy.WAIT);

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getName()).thenReturn("testMethod");
            when(joinPoint.getTarget()).thenReturn(new Object());
            when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class)))
                    .thenReturn(true);
            when(joinPoint.proceed()).thenThrow(new RuntimeException("业务异常"));
            when(redisTemplate.delete(anyString())).thenReturn(true);

            assertThrows(RuntimeException.class, () -> interceptor.around(joinPoint, annotation));

            // 验证删除了幂等 key
            verify(redisTemplate, atLeastOnce()).delete(anyString());
        }
    }

    @Nested
    @DisplayName("Key 构建")
    class KeyBuilding {

        @Test
        @DisplayName("指定 keyPrefix → 使用指定的前缀")
        void shouldUseSpecifiedPrefix() throws Throwable {
            Idempotent annotation = mockIdempotent("payment:callback", new String[0],
                    3600, 0, false, Idempotent.ConcurrentStrategy.WAIT);

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getName()).thenReturn("testMethod");
            when(joinPoint.getTarget()).thenReturn(new Object());
            when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class)))
                    .thenReturn(true);
            when(joinPoint.proceed()).thenReturn("ok");

            interceptor.around(joinPoint, annotation);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOperations).setIfAbsent(keyCaptor.capture(), eq("PROCESSING"), any(Duration.class));
            assertTrue(keyCaptor.getValue().startsWith("idempotent:payment:callback"));
        }

        @Test
        @DisplayName("空 keyPrefix → 使用类名:方法名作为前缀")
        void shouldUseClassNameAndMethodNameWhenPrefixEmpty() throws Throwable {
            Idempotent annotation = mockIdempotent("", new String[0],
                    3600, 0, false, Idempotent.ConcurrentStrategy.WAIT);

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getName()).thenReturn("handlePayment");
            when(joinPoint.getTarget()).thenReturn(new PaymentController());
            when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class)))
                    .thenReturn(true);
            when(joinPoint.proceed()).thenReturn("ok");

            interceptor.around(joinPoint, annotation);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOperations).setIfAbsent(keyCaptor.capture(), eq("PROCESSING"), any(Duration.class));
            String key = keyCaptor.getValue();
            assertTrue(key.startsWith("idempotent:PaymentController:handlePayment"),
                    "Expected key to start with 'idempotent:PaymentController:handlePayment' but got: " + key);
        }

        @Test
        @DisplayName("keyPrefix + staticKey → 拼接 staticKey")
        void shouldAppendStaticKey() throws Throwable {
            Idempotent annotation = mockIdempotentWithStaticKey("order", "create", new String[0],
                    3600, 0, false, Idempotent.ConcurrentStrategy.WAIT);

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getName()).thenReturn("testMethod");
            when(joinPoint.getTarget()).thenReturn(new Object());
            when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class)))
                    .thenReturn(true);
            when(joinPoint.proceed()).thenReturn("ok");

            interceptor.around(joinPoint, annotation);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOperations).setIfAbsent(keyCaptor.capture(), eq("PROCESSING"), any(Duration.class));
            String key = keyCaptor.getValue();
            assertTrue(key.startsWith("idempotent:order:create"),
                    "Expected key to start with 'idempotent:order:create' but got: " + key);
        }

        @Test
        @DisplayName("keyPrefix + fields → 拼装字段值")
        void shouldAppendFieldValues() throws Throwable {
            Idempotent annotation = mockIdempotent("order", new String[]{"orderId"},
                    3600, 0, false, Idempotent.ConcurrentStrategy.WAIT);

            // 创建一个带 getOrderId() 方法的请求对象
            var request = new Object() {
                @SuppressWarnings("unused")
                public String getOrderId() { return "ORD-12345"; }
            };

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getName()).thenReturn("testMethod");
            when(joinPoint.getTarget()).thenReturn(new Object());
            when(joinPoint.getArgs()).thenReturn(new Object[]{request});
            when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class)))
                    .thenReturn(true);
            when(joinPoint.proceed()).thenReturn("ok");

            interceptor.around(joinPoint, annotation);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOperations).setIfAbsent(keyCaptor.capture(), eq("PROCESSING"), any(Duration.class));
            String key = keyCaptor.getValue();
            assertTrue(key.startsWith("idempotent:order:ORD-12345"),
                    "Expected key to start with 'idempotent:order:ORD-12345' but got: " + key);
        }

        @Test
        @DisplayName("fields 为 null args → 不拼接字段值")
        void shouldHandleNullArgs() throws Throwable {
            Idempotent annotation = mockIdempotent("order", new String[]{"orderId"},
                    3600, 0, false, Idempotent.ConcurrentStrategy.WAIT);

            when(joinPoint.getSignature()).thenReturn(methodSignature);
            when(methodSignature.getName()).thenReturn("testMethod");
            when(joinPoint.getTarget()).thenReturn(new Object());
            when(joinPoint.getArgs()).thenReturn(null); // args 为 null
            when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class)))
                    .thenReturn(true);
            when(joinPoint.proceed()).thenReturn("ok");

            // 不应抛异常
            Object result = interceptor.around(joinPoint, annotation);
            assertEquals("ok", result);
        }
    }

    /**
     * 测试用的 Controller 类，用于验证空 keyPrefix 时的类名回退。
     */
    static class PaymentController {
    }
}
