package com.hotel.common.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class TokenService {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String REFRESH_PREFIX = "token:refresh:";
    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    public record TokenPair(String accessToken, String refreshToken) {}

    public TokenPair generate(String userId, String name, List<String> roles) {
        String access = jwtUtil.generateToken(userId, name, roles, null);
        String refresh = UUID.randomUUID().toString();
        try {
            UserInfo info = new UserInfo(userId, name, roles);
            redisTemplate.opsForValue().set(REFRESH_PREFIX + refresh,
                    objectMapper.writeValueAsString(info), Duration.ofDays(7));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化用户信息失败", e);
        }
        return new TokenPair(access, refresh);
    }

    public TokenPair refresh(String refreshToken) {
        String json = redisTemplate.opsForValue().get(REFRESH_PREFIX + refreshToken);
        if (json == null) {
            throw new RuntimeException("RefreshToken 已过期");
        }
        try {
            UserInfo info = objectMapper.readValue(json, UserInfo.class);
            String newAccess = jwtUtil.generateToken(info.userId, info.name, info.roles, null);
            return new TokenPair(newAccess, refreshToken);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("解析用户信息失败", e);
        }
    }

    public void revoke(String token) {
        try {
            var claims = jwtUtil.parseClaims(token);
            long ttl = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
            if (ttl > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1", Duration.ofSeconds(ttl));
            }
        } catch (Exception e) {
            log.warn("吊销 Token 失败: {}", e.getMessage());
        }
    }

    private record UserInfo(String userId, String name, List<String> roles) {}
}
