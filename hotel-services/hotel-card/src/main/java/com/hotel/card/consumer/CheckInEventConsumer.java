package com.hotel.card.consumer;

import com.hotel.card.model.dto.CreateCardRequest;
import com.hotel.card.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 入住事件消费者。监听入住/续住/退房事件，执行制卡/延期/注销。
 * 缺陷10: Kafka 消费可靠性 - 手动确认。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CheckInEventConsumer {

    private final CardService cardService;

    @KafkaListener(topics = "checkin-events", groupId = "card-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleCheckInEvent(Map<String, Object> event, Acknowledgment ack) {
        try {
            String eventType = (String) event.get("eventType");
            String checkinId = (String) event.get("checkinId");
            String hotelId = (String) event.get("hotelId");
            String roomId = (String) event.get("roomId");
            String roomNo = (String) event.get("roomNo");

            log.info("收到入住事件: eventType={}, checkinId={}, roomId={}", eventType, checkinId, roomId);

            switch (eventType) {
                case "CHECKED_IN" -> {
                    // 入住 → 制作门卡
                    CreateCardRequest req = new CreateCardRequest();
                    req.setCheckinId(checkinId);
                    req.setHotelId(hotelId);
                    req.setRoomId(roomId);
                    req.setRoomNo(roomNo);
                    req.setCheckInTime(LocalDateTime.now());
                    req.setCheckOutTime(LocalDateTime.now().plusDays(1));
                    cardService.createCard(req);
                }
                case "EXTENDED" -> {
                    // 续住 → 延期门卡
                    var cards = cardService.queryCardsByCheckinId(checkinId);
                    if (!cards.isEmpty()) {
                        String newValidToStr = (String) event.get("checkOutTime");
                        LocalDateTime newValidTo = newValidToStr != null
                                ? LocalDateTime.parse(newValidToStr)
                                : LocalDateTime.now().plusDays(1);
                        cardService.extendCard(cards.get(0).getCardId(), newValidTo);
                    }
                }
                case "CHECKED_OUT" -> {
                    // 退房 → 注销所有门卡
                    cardService.cancelAllByCheckinId(checkinId);
                }
                default -> log.info("未知事件类型: {}", eventType);
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("处理入住事件失败: {}", event, e);
            throw e;
        }
    }
}
