package com.hotel.common.idempotent;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Aspect
@Slf4j
public class IdempotentInterceptor {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String PREFIX = "idempotent:";
    private static final String RESULT_PREFIX = "idempotent:result:";
    private static final String PROCESSING = "PROCESSING";
    private static final String SUCCESS = "SUCCESS";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint point, Idempotent idempotent) throws Throwable {
        String key = buildKey(point, idempotent);
        String valKey = PREFIX + key;

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(valKey, PROCESSING, Duration.ofSeconds(idempotent.expireSeconds()));

        if (Boolean.FALSE.equals(acquired)) {
            return handleConcurrent(key, valKey, idempotent);
        }

        try {
            Object result = point.proceed();
            if (idempotent.deleteOnSuccess()) {
                redisTemplate.delete(valKey);
            } else {
                redisTemplate.opsForValue().set(valKey, SUCCESS, Duration.ofSeconds(idempotent.expireSeconds()));
                if (result != null) {
                    cacheResult(key, result, idempotent.expireSeconds());
                }
            }
            return result;
        } catch (Exception e) {
            redisTemplate.delete(valKey);
            deleteResult(key);
            throw e;
        }
    }

    private Object handleConcurrent(String key, String valKey, Idempotent idempotent) throws Throwable {
        String status = redisTemplate.opsForValue().get(valKey);
        if (SUCCESS.equals(status)) {
            Object cached = getCachedResult(key);
            if (cached != null) return cached;
        }
        return switch (idempotent.concurrentStrategy()) {
            case THROW -> throw new IdempotentException("请求正在处理中，请勿重复提交");
            case SKIP -> null;
            case WAIT -> waitForCompletion(key, idempotent.timeoutSeconds());
        };
    }

    private Object waitForCompletion(String key, long timeout) throws InterruptedException {
        long deadline = timeout > 0 ? System.currentTimeMillis() + timeout * 1000 : Long.MAX_VALUE;
        while (System.currentTimeMillis() < deadline) {
            String status = redisTemplate.opsForValue().get(PREFIX + key);
            if (SUCCESS.equals(status)) {
                Object r = getCachedResult(key);
                if (r != null) return r;
            }
            Thread.sleep(100);
        }
        throw new IdempotentException("处理超时，请稍后重试");
    }

    private String buildKey(ProceedingJoinPoint point, Idempotent idempotent) {
        StringBuilder sb = new StringBuilder();
        String prefix = idempotent.keyPrefix();
        if (prefix.isEmpty()) {
            prefix = point.getTarget().getClass().getSimpleName() + ":" + point.getSignature().getName();
        }
        sb.append(prefix);
        if (!idempotent.staticKey().isEmpty()) {
            sb.append(":").append(idempotent.staticKey());
        }
        if (idempotent.fields().length > 0) {
            Object[] args = point.getArgs();
            for (String f : idempotent.fields()) {
                Object val = getField(args, f);
                if (val != null) sb.append(":").append(val);
            }
        }
        return sb.toString();
    }

    private Object getField(Object[] args, String field) {
        if (args == null || args.length == 0) return null;
        try {
            Object req = args[0];
            if (req == null) return null;
            var method = req.getClass().getMethod("get" + Character.toUpperCase(field.charAt(0)) + field.substring(1));
            return method.invoke(req);
        } catch (Exception e) {
            return null;
        }
    }

    private void cacheResult(String key, Object result, long ttl) {
        try {
            redisTemplate.opsForValue().set(RESULT_PREFIX + key,
                    objectMapper.writeValueAsString(result), Duration.ofSeconds(ttl));
        } catch (Exception e) {
            log.warn("缓存幂等结果失败", e);
        }
    }

    private Object getCachedResult(String key) {
        try {
            String json = redisTemplate.opsForValue().get(RESULT_PREFIX + key);
            return json != null ? objectMapper.readValue(json, Object.class) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void deleteResult(String key) {
        try {
            redisTemplate.delete(RESULT_PREFIX + key);
        } catch (Exception ignored) {
        }
    }
}
