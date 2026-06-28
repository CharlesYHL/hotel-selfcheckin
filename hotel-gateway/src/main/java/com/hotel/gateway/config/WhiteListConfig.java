package com.hotel.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "gateway.white-list")
public class WhiteListConfig {

    private List<String> paths = List.of(
            "/api/auth/",
            "/api/sms/send",
            "/actuator",
            "/static"
    );

    public boolean isWhitePath(String path) {
        return paths.stream().anyMatch(path::startsWith);
    }
}
