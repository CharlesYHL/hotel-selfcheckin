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
        log.info("[Saga-PaymentStep] 开始执行支付步骤: sagaId={}, orderId={}, hotelId={}, amount={}",
                context.getSagaId(), context.getOrderId(),
                context.getAttributes().get("hotelId"), context.getAttributes().get("amount"));

        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setOrderId(context.getOrderId());
        request.setOrderNo(context.getOrderId());
        request.setHotelId(context.getAttributes().get("hotelId").toString());
        request.setAmount(new BigDecimal(context.getAttributes().get("amount").toString()));
        request.setPaymentType(1);
        request.setPayChannel("WX_PAY");

        log.info("[Saga-PaymentStep] 调用 createPayment: sagaId={}, orderId={}", context.getSagaId(), context.getOrderId());
        PaymentResponse result = paymentService.createPayment(request);
        context.setPaymentId(result.getPaymentId());
        log.info("[Saga-PaymentStep] 支付订单创建成功: sagaId={}, paymentId={}, paymentNo={}",
                context.getSagaId(), result.getPaymentId(), result.getPaymentNo());

        // 模拟支付成功回调
        com.hotel.payment.model.dto.PaymentCallbackRequest callback = new com.hotel.payment.model.dto.PaymentCallbackRequest();
        callback.setPaymentNo(result.getPaymentNo());
        callback.setTradeNo("TRADE" + System.currentTimeMillis());
        callback.setStatus(1);
        log.info("[Saga-PaymentStep] 模拟支付回调: sagaId={}, paymentNo={}, tradeNo={}",
                context.getSagaId(), result.getPaymentNo(), callback.getTradeNo());
        paymentService.handleCallback(callback);

        log.info("[Saga-PaymentStep] 支付步骤执行完成: sagaId={}, orderId={}, paymentId={}",
                context.getSagaId(), context.getOrderId(), result.getPaymentId());
        return result;
    }

    @Override
    public void compensate(SagaContext context) {
        log.info("[Saga-PaymentStep] 开始补偿: sagaId={}, paymentId={}", context.getSagaId(), context.getPaymentId());
        if (context.getPaymentId() == null) {
            log.warn("[Saga-PaymentStep] 跳过补偿: paymentId 为空，可能支付步骤未成功创建订单: sagaId={}", context.getSagaId());
            return;
        }

        log.info("[Saga-PaymentStep] 补偿退款: sagaId={}, paymentId={}", context.getSagaId(), context.getPaymentId());

        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setPaymentId(context.getPaymentId());
        refundRequest.setRefundNo("RFN_SAGA_" + context.getSagaId().replace("-", "").substring(0, 16));
        refundRequest.setRefundReason("入住办理失败，触发 Saga 补偿");
        refundRequest.setRefundAmount(null); // 将从 Payment 记录中取原金额
        refundRequest.setRefundType(1);

        // 查支付记录获取金额
        log.info("[Saga-PaymentStep] 查询支付记录获取退款金额: sagaId={}, paymentId={}", context.getSagaId(), context.getPaymentId());
        var payment = paymentService.queryPayment(context.getPaymentId());
        refundRequest.setRefundAmount(payment.getAmount());
        log.info("[Saga-PaymentStep] 退款金额确认: sagaId={}, paymentId={}, refundAmount={}",
                context.getSagaId(), context.getPaymentId(), payment.getAmount());

        paymentService.applyRefund(refundRequest);
        log.info("[Saga-PaymentStep] 补偿完成: sagaId={}, paymentId={}", context.getSagaId(), context.getPaymentId());
    }
}
