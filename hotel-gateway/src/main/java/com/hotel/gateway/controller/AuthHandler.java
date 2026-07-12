package com.hotel.gateway.controller;

import com.hotel.common.core.Result;
import com.hotel.common.security.TokenService;
import com.hotel.gateway.model.entity.User;
import com.hotel.gateway.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 认证响应式 Handler（替代 @RestController，与 WebFlux 兼容）
 * 路由在 GatewayRouterConfig 中注册
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthHandler {

    private final UserService userService;
    private final TokenService tokenService;

    public Mono<ServerResponse> login(ServerRequest request) {
        return request.bodyToMono(LoginRequest.class)
                .flatMap(req -> {
                    if (req.getUsername() == null || req.getPassword() == null) {
                        return ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Result.fail(400, "用户名和密码不能为空"));
                    }
                    Optional<User> userOpt = userService.authenticate(req.getUsername(), req.getPassword());
                    if (userOpt.isEmpty()) {
                        return ServerResponse.status(401)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Result.fail(401, "用户名或密码错误"));
                    }
                    User user = userOpt.get();
                    TokenService.TokenPair tokens = tokenService.generate(
                            user.getUserId(), user.getName(), List.of(user.getRole()));

                    LoginResponse resp = new LoginResponse();
                    resp.setAccessToken(tokens.accessToken());
                    resp.setRefreshToken(tokens.refreshToken());
                    resp.setUserId(user.getUserId());
                    resp.setName(user.getName());
                    resp.setRole(user.getRole());
                    resp.setPhone(user.getPhone());

                    log.info("用户登录成功: username={}, role={}", user.getUsername(), user.getRole());
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Result.success(resp));
                });
    }

    public Mono<ServerResponse> refresh(ServerRequest request) {
        return request.bodyToMono(RefreshRequest.class)
                .flatMap(req -> {
                    if (req.getRefreshToken() == null) {
                        return ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Result.fail(400, "refreshToken 不能为空"));
                    }
                    try {
                        TokenService.TokenPair tokens = tokenService.refresh(req.getRefreshToken());
                        return ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Result.success(Map.of(
                                        "accessToken", tokens.accessToken(),
                                        "refreshToken", tokens.refreshToken()
                                )));
                    } catch (Exception e) {
                        return ServerResponse.status(401)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Result.fail(401, "RefreshToken 无效或已过期"));
                    }
                });
    }

    public Mono<ServerResponse> logout(ServerRequest request) {
        String auth = request.headers().firstHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            tokenService.revoke(auth.substring(7));
        }
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Result.success());
    }

    public Mono<ServerResponse> me(ServerRequest request) {
        String userId = request.headers().firstHeader("X-User-Id");
        if (userId == null) {
            return ServerResponse.status(401)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Result.fail(401, "未登录"));
        }
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            return ServerResponse.status(404)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Result.fail(404, "用户不存在"));
        }
        User u = userOpt.get();
        UserView v = new UserView();
        v.setUserId(u.getUserId());
        v.setUsername(u.getUsername());
        v.setName(u.getName());
        v.setRole(u.getRole());
        v.setPhone(u.getPhone());
        v.setAvatar(u.getAvatar());
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Result.success(v));
    }

    // ============= DTOs =============

    public static class LoginRequest {
        private String username;
        private String password;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginResponse {
        private String accessToken;
        private String refreshToken;
        private String userId;
        private String name;
        private String role;
        private String phone;
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    public static class RefreshRequest {
        private String refreshToken;
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    public static class UserView {
        private String userId;
        private String username;
        private String name;
        private String role;
        private String phone;
        private String avatar;
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
    }
}
