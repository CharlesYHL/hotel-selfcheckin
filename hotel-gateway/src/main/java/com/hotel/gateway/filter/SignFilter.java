package com.hotel.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import com.hotel.gateway.config.WhiteListConfig;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class SignFilter implements GlobalFilter, Ordered {

    @Value("${sign.secret:hotel-sign-secret-change-in-production}")
    private String signSecret;

    @Value("${sign.enabled:true}")
    private boolean signEnabled;

    @Value("${sign.maxAge:300000}")
    private long maxAge;

    @Autowired
    private WhiteListConfig whiteListConfig;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!signEnabled) return chain.filter(exchange);

        String path = exchange.getRequest().getPath().value();
        if (whiteListConfig.isWhitePath(path)) return chain.filter(exchange);

        String sign = exchange.getRequest().getHeaders().getFirst("X-Sign");
        String timestamp = exchange.getRequest().getHeaders().getFirst("X-Timestamp");
        String appId = exchange.getRequest().getHeaders().getFirst("X-App-Id");

        if (sign == null || timestamp == null || appId == null) {
            return badRequest(exchange, "缺少签名参数 X-Sign, X-Timestamp, X-App-Id");
        }

        try {
            long ts = Long.parseLong(timestamp);
            if (Math.abs(System.currentTimeMillis() - ts) > maxAge) {
                return badRequest(exchange, "请求已过期");
            }
        } catch (NumberFormatException e) {
            return badRequest(exchange, "X-Timestamp 格式错误");
        }

        String expected = HmacUtils.hmacSha256Hex(signSecret, appId + path + timestamp);
        if (!secureEquals(sign, expected)) {
            return unauthorized(exchange, "签名验证失败");
        }

        return chain.filter(exchange);
    }

    private boolean secureEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }

    private Mono<Void> badRequest(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = String.format("{\"code\":-1,\"message\":\"%s\"}", message);
        DataBuffer buf = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buf));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = String.format("{\"code\":-1,\"message\":\"%s\"}", message);
        DataBuffer buf = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buf));
    }

    @Override
    public int getOrder() {
        return -90;
    }
}
