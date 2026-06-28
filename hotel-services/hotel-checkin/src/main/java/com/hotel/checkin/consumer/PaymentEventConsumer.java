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
            if (!"PAID".equals(eventType)) {
                ack.acknowledge();
                return;
            }

            String orderId = (String) event.get("orderId");
            String hotelId = (String) event.get("hotelId");
            log.info("收到支付成功事件: orderId={}", orderId);

            // 检查是否已入住
            CheckIn existing = checkInMapper.selectByOrderId(orderId);
            if (existing != null) {
                log.info("订单已入住，跳过: orderId={}", orderId);
                ack.acknowledge();
                return;
            }

            // 自动办理入住（简化流程）
            CheckInRequest request = new CheckInRequest();
            request.setOrderId(orderId);
            request.setHotelId(hotelId);
            request.setCheckinChannel(1);
            checkInService.checkIn(request);

            ack.acknowledge();
        } catch (Exception e) {
            log.error("处理支付事件失败: {}", event, e);
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
