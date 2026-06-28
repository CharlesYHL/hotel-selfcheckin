package com.hotel.gateway.filter;

import com.hotel.common.security.JwtUtil;
import com.hotel.gateway.config.WhiteListConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class AuthFilter implements GlobalFilter, Ordered {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WhiteListConfig whiteListConfig;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (whiteListConfig.isWhitePath(path)) {
            return chain.filter(exchange);
        }

        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return writeResponse(exchange, HttpStatus.UNAUTHORIZED, "缺少认证令牌");
        }

        String token = auth.substring(7);
        try {
            if (!jwtUtil.validateToken(token)) {
                return writeResponse(exchange, HttpStatus.UNAUTHORIZED, "令牌无效或已过期");
            }
            Claims claims = jwtUtil.parseClaims(token);
            List<String> roles = jwtUtil.getRoles(token);

            if (!hasPermission(path, roles)) {
                return writeResponse(exchange, HttpStatus.FORBIDDEN, "权限不足");
            }

            ServerHttpRequest req = exchange.getRequest().mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Name", claims.get("name", String.class))
                    .header("X-User-Roles", String.join(",", roles))
                    .build();
            return chain.filter(exchange.mutate().request(req).build());

        } catch (ExpiredJwtException e) {
            return writeResponse(exchange, HttpStatus.UNAUTHORIZED, "令牌已过期");
        } catch (JwtException e) {
            return writeResponse(exchange, HttpStatus.UNAUTHORIZED, "令牌解析失败");
        }
    }

    private boolean hasPermission(String path, List<String> roles) {
        if (roles.contains("ROLE_ADMIN")) return true;
        if (roles.contains("ROLE_MEMBER")
                && (path.startsWith("/api/order") || path.startsWith("/api/pay") || path.startsWith("/api/checkin")))
            return true;
        if (roles.contains("ROLE_STAFF")
                && (path.startsWith("/api/room") || path.startsWith("/api/checkin") || path.startsWith("/api/member")))
            return true;
        return false;
    }

    private Mono<Void> writeResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = String.format("{\"code\":%d,\"message\":\"%s\"}", status.value(), message);
        DataBuffer buf = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buf));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
