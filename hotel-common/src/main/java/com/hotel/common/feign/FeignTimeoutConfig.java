package com.hotel.common.feign;

import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignTimeoutConfig {

    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(
                (int) java.util.concurrent.TimeUnit.SECONDS.toMillis(3),
                (int) java.util.concurrent.TimeUnit.SECONDS.toMillis(5)
        );
    }
}
