package com.hotel.order.feign;

import com.hotel.common.core.Result;
import com.hotel.order.model.dto.PaymentCreateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "payment-service", url = "${feign.payment.url:http://localhost:8083}")
public interface PaymentFeignClient {

    @PostMapping("/api/pay/create")
    Result<Map<String, Object>> createPayment(@RequestBody PaymentCreateRequest request);
}
