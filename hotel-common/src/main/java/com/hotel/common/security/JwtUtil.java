package com.hotel.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret:hotel-selfcheckin-jwt-secret-key-min-256-bits!!}") String secret,
                   @Value("${jwt.expiration:86400000}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(String userId, String name, List<String> roles, Map<String, Object> extraClaims) {
        Date now = new Date();
        JwtBuilder builder = Jwts.builder()
                .subject(userId)
                .claim("name", name)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(secretKey);
        if (extraClaims != null) {
            extraClaims.forEach(builder::claim);
        }
        return builder.compact();
    }

    public String generateMemberToken(String memberId, String memberName, String level) {
        return generateToken(memberId, memberName, List.of("ROLE_MEMBER"),
                Map.of("memberId", memberId, "level", level));
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT 已过期: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("JWT 无效: {}", e.getMessage());
        }
        return false;
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUserId(String token) {
        return parseClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        return parseClaims(token).get("roles", List.class);
    }

    public boolean isExpiringSoon(String token) {
        try {
            return parseClaims(token).getExpiration().getTime() - System.currentTimeMillis() < 3600000;
        } catch (Exception e) {
            return false;
        }
    }
}
