package com.hotel.common.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtil 单元测试")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET = "test-jwt-secret-key-minimum-256-bits-long-for-hs256!!";
    private static final long EXPIRATION = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION);
    }

    @Nested
    @DisplayName("Token 生成与校验")
    class TokenGenerationAndValidation {

        @Test
        @DisplayName("正常生成 Token 并校验通过")
        void shouldGenerateAndValidateToken() {
            String token = jwtUtil.generateToken("U001", "张三", List.of("ROLE_MEMBER"), null);

            assertNotNull(token);
            assertTrue(jwtUtil.validateToken(token));
        }

        @Test
        @DisplayName("generateMemberToken 应包含 ROLE_MEMBER 角色和会员信息")
        void shouldGenerateMemberTokenWithCorrectClaims() {
            String token = jwtUtil.generateMemberToken("M001", "李四", "GOLD");

            assertTrue(jwtUtil.validateToken(token));
            assertEquals("M001", jwtUtil.getUserId(token));
            List<String> roles = jwtUtil.getRoles(token);
            assertTrue(roles.contains("ROLE_MEMBER"));
        }

        @Test
        @DisplayName("getUserId 应返回正确的 subject")
        void shouldReturnCorrectUserId() {
            String token = jwtUtil.generateToken("U002", "王五", List.of("ROLE_ADMIN"), null);
            assertEquals("U002", jwtUtil.getUserId(token));
        }

        @Test
        @DisplayName("getRoles 应返回正确的角色列表")
        void shouldReturnCorrectRoles() {
            String token = jwtUtil.generateToken("U003", "赵六",
                    List.of("ROLE_MEMBER", "ROLE_STAFF"), null);

            List<String> roles = jwtUtil.getRoles(token);
            assertEquals(2, roles.size());
            assertTrue(roles.contains("ROLE_MEMBER"));
            assertTrue(roles.contains("ROLE_STAFF"));
        }

        @Test
        @DisplayName("extraClaims 应正确写入 Token")
        void shouldIncludeExtraClaims() {
            Map<String, Object> extra = Map.of("hotelId", "H001", "level", "PLATINUM");
            String token = jwtUtil.generateToken("U004", "测试", List.of("ROLE_MEMBER"), extra);

            Claims claims = jwtUtil.parseClaims(token);
            assertEquals("H001", claims.get("hotelId"));
            assertEquals("PLATINUM", claims.get("level"));
        }
    }

    @Nested
    @DisplayName("Token 过期处理")
    class TokenExpiration {

        @Test
        @DisplayName("过期 Token 应校验失败")
        void shouldRejectExpiredToken() {
            // 创建一个已过期的 JwtUtil（过期时间 = 0）
            JwtUtil expiredJwtUtil = new JwtUtil(SECRET, 0L);
            String token = expiredJwtUtil.generateToken("U005", "过期用户", List.of("ROLE_MEMBER"), null);

            assertFalse(jwtUtil.validateToken(token));
        }

        @Test
        @DisplayName("isExpiringSoon: 剩余 < 1h 应返回 true")
        void shouldDetectExpiringSoon() {
            // 创建过期时间为 30 分钟的 JwtUtil
            JwtUtil shortExpiryJwtUtil = new JwtUtil(SECRET, 1800000L); // 30 min
            String token = shortExpiryJwtUtil.generateToken("U006", "即将过期", List.of("ROLE_MEMBER"), null);

            assertTrue(jwtUtil.isExpiringSoon(token));
        }

        @Test
        @DisplayName("isExpiringSoon: 剩余 > 1h 应返回 false")
        void shouldNotFlagLongLivedToken() {
            // 使用 2h 过期时间确保不在即将过期窗口内
            JwtUtil longExpiryUtil = new JwtUtil(SECRET, 7200000L); // 2 hours
            String token = longExpiryUtil.generateToken("U007", "长期有效", List.of("ROLE_MEMBER"), null);

            assertFalse(longExpiryUtil.isExpiringSoon(token));
        }
    }

    @Nested
    @DisplayName("异常 Token 处理")
    class InvalidTokenHandling {

        @Test
        @DisplayName("null Token 应校验失败")
        void shouldRejectNullToken() {
            assertThrows(IllegalArgumentException.class, () -> jwtUtil.validateToken(null));
        }

        @Test
        @DisplayName("空字符串 Token 应校验失败")
        void shouldRejectEmptyToken() {
            assertThrows(IllegalArgumentException.class, () -> jwtUtil.validateToken(""));
        }

        @Test
        @DisplayName("伪造签名 Token 应校验失败")
        void shouldRejectTamperedToken() {
            String token = jwtUtil.generateToken("U008", "合法用户", List.of("ROLE_MEMBER"), null);
            // 篡改 token 的最后一个字符
            String tampered = token.substring(0, token.length() - 1)
                    + (token.charAt(token.length() - 1) == 'A' ? 'B' : 'A');

            assertFalse(jwtUtil.validateToken(tampered));
        }

        @Test
        @DisplayName("不同密钥签名的 Token 应校验失败")
        void shouldRejectDifferentKeyToken() {
            JwtUtil otherJwtUtil = new JwtUtil(
                    "other-secret-key-that-is-also-256-bits-long!!", 3600000L);
            String token = otherJwtUtil.generateToken("U009", "其他密钥", List.of("ROLE_MEMBER"), null);

            assertFalse(jwtUtil.validateToken(token));
        }
    }
}
