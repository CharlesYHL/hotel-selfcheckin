package com.hotel.checkin.consumer;

import com.hotel.checkin.model.dto.CheckInRequest;
import com.hotel.checkin.model.entity.CheckIn;
import com.hotel.checkin.model.enums.CheckInStatus;
import com.hotel.checkin.repository.CheckInMapper;
import com.hotel.checkin.service.CheckInService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 支付事件消费者。监听支付成功事件，自动触发入住流程。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final CheckInService checkInService;
    private final CheckInMapper checkInMapper;

    /**
     * 监听支付成功事件，自动办理入住。
     * 缺陷10: Kafka 消费可靠性 - 手动确认 + DLQ。
     */
    @KafkaListener(topics = "payment-events", groupId = "checkin-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void handlePaymentEvent(Map<String, Object> event, Acknowledgment ack) {
        try {
            String eventType = (String) event.get("eventType");
            String orderId = (String) event.get("orderId");
            String paymentId = (String) event.get("paymentId");
            String paymentNo = (String) event.get("paymentNo");

            log.info("[PaymentEventConsumer] 收到支付事件: eventType={}, orderId={}, paymentId={}, paymentNo={}",
                    eventType, orderId, paymentId, paymentNo);

            if (!"PAID".equals(eventType)) {
                log.info("[PaymentEventConsumer] 非PAID事件，跳过处理: eventType={}, orderId={}", eventType, orderId);
                ack.acknowledge();
                return;
            }

            String hotelId = (String) event.get("hotelId");
            log.info("[PaymentEventConsumer] 收到支付成功事件，开始办理入住: orderId={}, paymentId={}, hotelId={}",
                    orderId, paymentId, hotelId);

            // 检查是否已入住
            CheckIn existing = checkInMapper.selectByOrderId(orderId);
            if (existing != null) {
                log.info("[PaymentEventConsumer] 订单已入住，跳过: orderId={}, checkinId={}, status={}",
                        orderId, existing.getCheckinId(), existing.getStatus());
                ack.acknowledge();
                return;
            }
            log.info("[PaymentEventConsumer] 订单未入住，继续办理: orderId={}", orderId);

            // 自动办理入住（简化流程）
            CheckInRequest request = new CheckInRequest();
            request.setOrderId(orderId);
            request.setHotelId(hotelId);
            request.setCheckinChannel(1);
            log.info("[PaymentEventConsumer] 调用 checkInService.checkIn: orderId={}, hotelId={}", orderId, hotelId);
            checkInService.checkIn(request);
            log.info("[PaymentEventConsumer] 入住办理成功: orderId={}, paymentId={}", orderId, paymentId);

            ack.acknowledge();
            log.info("[PaymentEventConsumer] Kafka消息已确认: orderId={}, paymentId={}", orderId, paymentId);
        } catch (Exception e) {
            log.error("[PaymentEventConsumer] 处理支付事件失败，触发重试: {}", event, e);
            // 不确认消息，触发重试
            throw e;
        }
    }

    /**
     * 监听房态变更事件。
     */
    @KafkaListener(topics = "room-status-changes", groupId = "checkin-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleRoomStatusChange(Map<String, Object> event, Acknowledgment ack) {
        try {
            String changeType = (String) event.get("changeType");
            String roomId = (String) event.get("roomId");
            log.info("收到房态变更: roomId={}, changeType={}", roomId, changeType);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("处理房态变更事件失败: {}", event, e);
            throw e;
        }
    }
}
