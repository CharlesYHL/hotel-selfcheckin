package com.hotel.payment.saga;

import com.hotel.common.saga.SagaContext;
import com.hotel.common.saga.SagaStep;
import com.hotel.common.saga.SagaStepInterface;
import com.hotel.payment.model.dto.PaymentCreateRequest;
import com.hotel.payment.model.dto.PaymentResponse;
import com.hotel.payment.model.dto.RefundRequest;
import com.hotel.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@SagaStep(order = 1, name = "payOrder", timeoutSeconds = 30)
@RequiredArgsConstructor
@Slf4j
public class PaymentStep implements SagaStepInterface {

    private final PaymentService paymentService;

    @Override
    public Object execute(SagaContext context) {
        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setOrderId(context.getOrderId());
        request.setOrderNo(context.getOrderId());
        request.setHotelId(context.getAttributes().get("hotelId").toString());
        request.setAmount(new BigDecimal(context.getAttributes().get("amount").toString()));
        request.setPaymentType(1);
        request.setPayChannel("WX_PAY");

        PaymentResponse result = paymentService.createPayment(request);
        context.setPaymentId(result.getPaymentId());

        // 模拟支付成功回调
        com.hotel.payment.model.dto.PaymentCallbackRequest callback = new com.hotel.payment.model.dto.PaymentCallbackRequest();
        callback.setPaymentNo(result.getPaymentNo());
        callback.setTradeNo("TRADE" + System.currentTimeMillis());
        callback.setStatus(1);
        paymentService.handleCallback(callback);

        log.info("Saga 支付完成: sagaId={}, orderId={}, paymentId={}",
                context.getSagaId(), context.getOrderId(), result.getPaymentId());
        return result;
    }

    @Override
    public void compensate(SagaContext context) {
        if (context.getPaymentId() != null) {
            log.info("Saga 补偿退款: sagaId={}, paymentId={}", context.getSagaId(), context.getPaymentId());

            RefundRequest refundRequest = new RefundRequest();
            refundRequest.setPaymentId(context.getPaymentId());
            refundRequest.setRefundNo("RFN_SAGA_" + context.getSagaId().replace("-", "").substring(0, 16));
            refundRequest.setRefundReason("入住办理失败，触发 Saga 补偿");
            refundRequest.setRefundAmount(null); // 将从 Payment 记录中取原金额
            refundRequest.setRefundType(1);

            // 查支付记录获取金额
            var payment = paymentService.queryPayment(context.getPaymentId());
            refundRequest.setRefundAmount(payment.getAmount());

            paymentService.applyRefund(refundRequest);
        }
    }
}
