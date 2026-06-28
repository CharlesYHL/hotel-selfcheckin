package com.hotel.card.service;

import com.hotel.card.model.dto.*;
import com.hotel.card.model.entity.CardLog;
import com.hotel.card.model.entity.RoomCard;
import com.hotel.card.model.enums.CardActionType;
import com.hotel.card.model.enums.CardStatus;
import com.hotel.card.model.enums.CardType;
import com.hotel.card.model.event.CardEvent;
import com.hotel.card.repository.CardLogMapper;
import com.hotel.card.repository.RoomCardMapper;
import com.hotel.common.core.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CardService {

    private final RoomCardMapper cardMapper;
    private final CardLogMapper logMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${card.qr-expire-minutes:1440}")
    private int qrExpireMinutes;

    @Value("${card.max-copy-count:3}")
    private int maxCopyCount;

    /**
     * 创建门卡。入住成功后调用。
     */
    @Transactional
    public CardResponse createCard(CreateCardRequest request) {
        // 检查是否已有有效卡
        List<RoomCard> existing = cardMapper.selectByCheckinId(request.getCheckinId());
        long activeCount = existing.stream()
                .filter(c -> c.getCardStatus() == CardStatus.ACTIVE.getCode()).count();
        if (activeCount > 0) {
            throw new BusinessException("该入住已存在有效门卡");
        }

        RoomCard card = new RoomCard();
        card.setCardId("CRD" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        card.setCardNo(generateCardNo());
        card.setHotelId(request.getHotelId());
        card.setCheckinId(request.getCheckinId());
        card.setRoomId(request.getRoomId());
        card.setRoomNo(request.getRoomNo());
        card.setGuestId(request.getGuestId());
        card.setValidFrom(request.getCheckInTime() != null ? request.getCheckInTime() : LocalDateTime.now());
        card.setValidTo(request.getCheckOutTime() != null ? request.getCheckOutTime() : LocalDateTime.now().plusDays(1));
        card.setCardType(CardType.NEW.getCode());
        card.setCardStatus(CardStatus.ACTIVE.getCode());
        card.setOpenCount(0);
        card.setQrCode(generateQrCode(card.getCardNo()));
        card.setQrExpireTime(LocalDateTime.now().plusMinutes(qrExpireMinutes));
        card.setCreatedTime(LocalDateTime.now());
        card.setUpdatedTime(LocalDateTime.now());
        cardMapper.insert(card);

        // 记录操作日志
        writeLog(card.getCardId(), card.getCardNo(), card.getHotelId(), card.getRoomId(),
                card.getRoomNo(), CardActionType.CREATE.getCode(), null, "系统制卡");

        // 发送事件
        kafkaTemplate.send("card-events", CardEvent.builder()
                .cardId(card.getCardId())
                .cardNo(card.getCardNo())
                .checkinId(card.getCheckinId())
                .hotelId(card.getHotelId())
                .roomId(card.getRoomId())
                .roomNo(card.getRoomNo())
                .eventType("CREATED")
                .validTo(card.getValidTo())
                .timestamp(System.currentTimeMillis())
                .build());

        log.info("门卡制作成功: cardId={}, cardNo={}, checkinId={}, roomId={}",
                card.getCardId(), card.getCardNo(), card.getCheckinId(), card.getRoomId());

        return toResponse(card);
    }

    /**
     * 补办门卡（门卡丢失后重新制作）。
     */
    @Transactional
    public CardResponse replaceCard(String oldCardId, String operatorId) {
        RoomCard oldCard = cardMapper.selectById(oldCardId);
        if (oldCard == null) {
            throw new BusinessException("原门卡不存在");
        }

        // 挂失旧卡
        int updated = cardMapper.updateStatus(oldCardId,
                CardStatus.LOST.getCode(), CardStatus.ACTIVE.getCode());
        if (updated == 0) {
            throw new BusinessException("门卡状态异常，无法挂失");
        }

        writeLog(oldCardId, oldCard.getCardNo(), oldCard.getHotelId(), oldCard.getRoomId(),
                oldCard.getRoomNo(), CardActionType.LOST.getCode(), operatorId, "门卡挂失-补办前");

        // 检查补卡次数
        List<RoomCard> historyCards = cardMapper.selectByCheckinId(oldCard.getCheckinId());
        long copyCount = historyCards.stream()
                .filter(c -> c.getCardType() == CardType.REPLACE.getCode()).count();
        if (copyCount >= maxCopyCount) {
            throw new BusinessException("补卡次数已达上限(" + maxCopyCount + "次)");
        }

        // 制作新卡
        RoomCard newCard = new RoomCard();
        newCard.setCardId("CRD" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        newCard.setCardNo(generateCardNo());
        newCard.setHotelId(oldCard.getHotelId());
        newCard.setCheckinId(oldCard.getCheckinId());
        newCard.setRoomId(oldCard.getRoomId());
        newCard.setRoomNo(oldCard.getRoomNo());
        newCard.setGuestId(oldCard.getGuestId());
        newCard.setValidFrom(LocalDateTime.now());
        newCard.setValidTo(oldCard.getValidTo());
        newCard.setCardType(CardType.REPLACE.getCode());
        newCard.setCardStatus(CardStatus.ACTIVE.getCode());
        newCard.setOpenCount(0);
        newCard.setQrCode(generateQrCode(newCard.getCardNo()));
        newCard.setQrExpireTime(LocalDateTime.now().plusMinutes(qrExpireMinutes));
        newCard.setCreatedTime(LocalDateTime.now());
        newCard.setUpdatedTime(LocalDateTime.now());
        cardMapper.insert(newCard);

        writeLog(newCard.getCardId(), newCard.getCardNo(), newCard.getHotelId(), newCard.getRoomId(),
                newCard.getRoomNo(), CardActionType.REPLACE.getCode(), operatorId,
                "补办门卡, 原卡: " + oldCardId);

        kafkaTemplate.send("card-events", CardEvent.builder()
                .cardId(newCard.getCardId())
                .cardNo(newCard.getCardNo())
                .checkinId(newCard.getCheckinId())
                .hotelId(newCard.getHotelId())
                .roomId(newCard.getRoomId())
                .eventType("REPLACED")
                .validTo(newCard.getValidTo())
                .timestamp(System.currentTimeMillis())
                .build());

        log.info("门卡补办成功: oldCardId={}, newCardId={}, newCardNo={}", oldCardId, newCard.getCardId(), newCard.getCardNo());
        return toResponse(newCard);
    }

    /**
     * 延期门卡。续住时调用。
     */
    @Transactional
    public CardResponse extendCard(String cardId, LocalDateTime newValidTo) {
        RoomCard card = cardMapper.selectById(cardId);
        if (card == null) {
            throw new BusinessException("门卡不存在");
        }
        if (card.getCardStatus() != CardStatus.ACTIVE.getCode()) {
            throw new BusinessException("仅有效门卡可延期");
        }

        cardMapper.extendValidity(cardId, newValidTo);

        writeLog(cardId, card.getCardNo(), card.getHotelId(), card.getRoomId(),
                card.getRoomNo(), CardActionType.EXTEND.getCode(), null,
                "延期至: " + newValidTo);

        card.setValidTo(newValidTo);
        log.info("门卡延期成功: cardId={}, newValidTo={}", cardId, newValidTo);
        return toResponse(card);
    }

    /**
     * 注销门卡。退房时调用。
     */
    @Transactional
    public CardResponse cancelCard(String cardId, String operatorId) {
        RoomCard card = cardMapper.selectById(cardId);
        if (card == null) {
            throw new BusinessException("门卡不存在");
        }

        int updated = cardMapper.updateStatus(cardId,
                CardStatus.CANCELLED.getCode(), CardStatus.ACTIVE.getCode());
        if (updated == 0) {
            throw new BusinessException("门卡状态异常，无法注销");
        }

        writeLog(cardId, card.getCardNo(), card.getHotelId(), card.getRoomId(),
                card.getRoomNo(), CardActionType.CANCEL.getCode(), operatorId, "退房注销");

        kafkaTemplate.send("card-events", CardEvent.builder()
                .cardId(card.getCardId())
                .cardNo(card.getCardNo())
                .checkinId(card.getCheckinId())
                .hotelId(card.getHotelId())
                .roomId(card.getRoomId())
                .eventType("CANCELLED")
                .timestamp(System.currentTimeMillis())
                .build());

        card.setCardStatus(CardStatus.CANCELLED.getCode());
        log.info("门卡注销成功: cardId={}, cardNo={}", cardId, card.getCardNo());
        return toResponse(card);
    }

    /**
     * 模拟开门操作。
     */
    @Transactional
    public Map<String, Object> openDoor(String cardId, String device) {
        RoomCard card = cardMapper.selectById(cardId);
        if (card == null) {
            return Map.of("success", false, "message", "门卡不存在");
        }
        if (card.getCardStatus() != CardStatus.ACTIVE.getCode()) {
            writeLog(cardId, card.getCardNo(), card.getHotelId(), card.getRoomId(),
                    card.getRoomNo(), CardActionType.OPEN.getCode(), null,
                    "开门失败: 卡状态=" + CardStatus.of(card.getCardStatus()).getDesc());
            return Map.of("success", false, "message", "门卡无效: " + CardStatus.of(card.getCardStatus()).getDesc());
        }
        if (card.getValidTo().isBefore(LocalDateTime.now())) {
            return Map.of("success", false, "message", "门卡已过期");
        }

        cardMapper.incrementOpenCount(cardId);

        writeLog(cardId, card.getCardNo(), card.getHotelId(), card.getRoomId(),
                card.getRoomNo(), CardActionType.OPEN.getCode(), device, "开门成功");

        return Map.of("success", true, "message", "开门成功", "roomNo", card.getRoomNo());
    }

    /**
     * 查询门卡信息。
     */
    public CardResponse queryCard(String cardId) {
        RoomCard card = cardMapper.selectById(cardId);
        if (card == null) {
            throw new BusinessException("门卡不存在");
        }
        return toResponse(card);
    }

    /**
     * 查询入住关联的所有门卡。
     */
    public List<CardResponse> queryCardsByCheckinId(String checkinId) {
        return cardMapper.selectByCheckinId(checkinId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 查询门卡操作日志。
     */
    public List<Map<String, Object>> getCardLogs(String cardId) {
        return logMapper.selectByCardId(cardId).stream().map(l -> {
            Map<String, Object> m = new HashMap<>();
            m.put("logId", l.getLogId());
            m.put("actionType", l.getActionType());
            m.put("actionTypeDesc", CardActionType.values()[l.getActionType() - 1].getDesc());
            m.put("openDevice", l.getOpenDevice());
            m.put("openResult", l.getOpenResult());
            m.put("operatorId", l.getOperatorId());
            m.put("remark", l.getRemark());
            m.put("createdTime", l.getCreatedTime());
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * 按入住ID注销所有有效门卡（退房场景）。
     */
    @Transactional
    public void cancelAllByCheckinId(String checkinId) {
        List<RoomCard> cards = cardMapper.selectByCheckinId(checkinId);
        for (RoomCard card : cards) {
            if (card.getCardStatus() == CardStatus.ACTIVE.getCode()) {
                cardMapper.updateStatus(card.getCardId(),
                        CardStatus.CANCELLED.getCode(), CardStatus.ACTIVE.getCode());
                writeLog(card.getCardId(), card.getCardNo(), card.getHotelId(), card.getRoomId(),
                        card.getRoomNo(), CardActionType.CANCEL.getCode(), null, "退房批量注销");
            }
        }
        log.info("批量注销门卡完成: checkinId={}, count={}", checkinId, cards.size());
    }

    private void writeLog(String cardId, String cardNo, String hotelId, String roomId, String roomNo,
                          int actionType, String device, String remark) {
        CardLog logEntry = new CardLog();
        logEntry.setLogId("CL" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        logEntry.setCardId(cardId);
        logEntry.setCardNo(cardNo);
        logEntry.setHotelId(hotelId);
        logEntry.setRoomId(roomId);
        logEntry.setRoomNo(roomNo);
        logEntry.setActionType(actionType);
        logEntry.setOpenDevice(device);
        logEntry.setOpenResult(actionType == CardActionType.OPEN.getCode() ? 1 : null);
        logEntry.setOperatorId(null);
        logEntry.setOperatorType(1);
        logEntry.setRemark(remark);
        logEntry.setCreatedTime(LocalDateTime.now());
        logMapper.insert(logEntry);
    }

    private String generateCardNo() {
        return "CARD" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
    }

    private String generateQrCode(String cardNo) {
        return "QR_" + cardNo + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private CardResponse toResponse(RoomCard c) {
        CardType ct = CardType.values()[c.getCardType() - 1];
        CardStatus cs = CardStatus.of(c.getCardStatus());
        return CardResponse.builder()
                .cardId(c.getCardId())
                .cardNo(c.getCardNo())
                .hotelId(c.getHotelId())
                .checkinId(c.getCheckinId())
                .roomId(c.getRoomId())
                .roomNo(c.getRoomNo())
                .validFrom(c.getValidFrom())
                .validTo(c.getValidTo())
                .cardType(c.getCardType())
                .cardTypeDesc(ct.getDesc())
                .cardStatus(c.getCardStatus())
                .cardStatusDesc(cs.getDesc())
                .openCount(c.getOpenCount())
                .qrCode(c.getQrCode())
                .createdTime(c.getCreatedTime())
                .build();
    }
}
