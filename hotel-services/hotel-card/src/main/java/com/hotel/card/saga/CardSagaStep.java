package com.hotel.card.saga;

import com.hotel.card.model.entity.RoomCard;
import com.hotel.card.model.enums.CardStatus;
import com.hotel.card.repository.RoomCardMapper;
import com.hotel.common.saga.SagaContext;
import com.hotel.common.saga.SagaStep;
import com.hotel.common.saga.SagaStepInterface;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Saga 步骤：制卡服务。
 * execute: 制卡完成（实际制卡在 CheckInEventConsumer 中执行）
 * compensate: 注销门卡
 */
@Component
@Slf4j
@RequiredArgsConstructor
@SagaStep(order = 4, name = "cardStep", timeoutSeconds = 30)
public class CardSagaStep implements SagaStepInterface {

    private final RoomCardMapper cardMapper;

    @Override
    public Object execute(SagaContext context) {
        String checkinId = (String) context.getAttributes().get("checkinId");
        if (checkinId == null) {
            log.warn("Saga 上下文中无 checkinId，跳过制卡步骤");
            return true;
        }
        log.info("Saga 执行: cardStep, checkinId={}", checkinId);
        return true;
    }

    @Override
    public void compensate(SagaContext context) {
        String checkinId = (String) context.getAttributes().get("checkinId");
        if (checkinId == null) {
            return;
        }
        List<RoomCard> cards = cardMapper.selectByCheckinId(checkinId);
        for (RoomCard card : cards) {
            if (card.getCardStatus() == CardStatus.ACTIVE.getCode()) {
                cardMapper.updateStatus(card.getCardId(),
                        CardStatus.CANCELLED.getCode(), CardStatus.ACTIVE.getCode());
                log.info("Saga 补偿: 注销门卡, cardId={}", card.getCardId());
            }
        }
    }
}
