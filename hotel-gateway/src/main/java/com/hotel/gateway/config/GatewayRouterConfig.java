package com.hotel.gateway.config;

import com.hotel.gateway.controller.AuthHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Map;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

@Configuration
@RequiredArgsConstructor
public class GatewayRouterConfig {

    private final AuthHandler authHandler;

    @Bean
    public RouterFunction<ServerResponse> rootRoute() {
        return RouterFunctions.route(GET("/"), request ->
                ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(
                                "code", 200,
                                "message", "酒店自助入住系统 API Gateway",
                                "services", Map.of(
                                        "order", "/api/order/**",
                                        "room", "/api/room/**",
                                        "payment", "/api/pay/**",
                                        "checkin", "/api/checkin/**",
                                        "card", "/api/card/**",
                                        "member", "/api/member/**",
                                        "auth", "/api/auth/**"
                                )
                        ))
        );
    }

    /**
     * 认证路由（响应式 Handler，与 WebFlux 兼容）
     */
    @Bean
    public RouterFunction<ServerResponse> authRoute() {
        return RouterFunctions.route(POST("/api/auth/login"), authHandler::login)
                .andRoute(POST("/api/auth/refresh"), authHandler::refresh)
                .andRoute(POST("/api/auth/logout"), authHandler::logout)
                .andRoute(GET("/api/auth/me"), authHandler::me);
    }
}
