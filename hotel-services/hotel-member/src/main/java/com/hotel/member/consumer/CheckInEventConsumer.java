package com.hotel.member.consumer;

import com.hotel.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 入住事件消费者。监听入住事件，自动奖励积分。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CheckInEventConsumer {

    private final MemberService memberService;

    @KafkaListener(topics = "checkin-events", groupId = "member-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleCheckInEvent(Map<String, Object> event, Acknowledgment ack) {
        try {
            String eventType = (String) event.get("eventType");
            if (!"CHECKED_IN".equals(eventType)) {
                ack.acknowledge();
                return;
            }

            String memberId = (String) event.get("memberId");
            String hotelId = (String) event.get("hotelId");
            String orderId = (String) event.get("orderId");
            String orderNo = (String) event.get("orderNo");
            String checkinId = (String) event.get("checkinId");

            if (memberId == null) {
                log.info("非会员入住，跳过积分奖励: checkinId={}", checkinId);
                ack.acknowledge();
                return;
            }

            log.info("收到入住事件，发放积分: memberId={}, checkinId={}", memberId, checkinId);

            memberService.earnCheckinPoints(memberId, hotelId, orderId, orderNo, checkinId,
                    BigDecimal.valueOf(500));

            ack.acknowledge();
        } catch (Exception e) {
            log.error("处理入住积分事件失败: {}", event, e);
            throw e;
        }
    }

    @KafkaListener(topics = "payment-events", groupId = "member-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void handlePaymentEvent(Map<String, Object> event, Acknowledgment ack) {
        try {
            String eventType = (String) event.get("eventType");
            log.debug("收到支付事件: eventType={}", eventType);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("处理支付事件失败: {}", event, e);
            throw e;
        }
    }
}
