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
 * doStep: 制卡完成（实际制卡在 CheckInEventConsumer 中执行）
 * compensate: 注销门卡
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CardSagaStep implements SagaStepInterface {

    private final RoomCardMapper cardMapper;

    @Override
    @SagaStep(name = "cardStep", compensateMethod = "compensateCard", timeoutSeconds = 30)
    public boolean doStep(SagaContext context) {
        String checkinId = (String) context.get("checkinId");
        if (checkinId == null) {
            log.warn("Saga 上下文中无 checkinId，跳过制卡步骤");
            return true;
        }
        log.info("Saga 执行: cardStep, checkinId={}", checkinId);
        return true;
    }

    public boolean compensateCard(SagaContext context) {
        String checkinId = (String) context.get("checkinId");
        if (checkinId == null) {
            return true;
        }
        List<RoomCard> cards = cardMapper.selectByCheckinId(checkinId);
        for (RoomCard card : cards) {
            if (card.getCardStatus() == CardStatus.ACTIVE.getCode()) {
                cardMapper.updateStatus(card.getCardId(),
                        CardStatus.CANCELLED.getCode(), CardStatus.ACTIVE.getCode());
                log.info("Saga 补偿: 注销门卡, cardId={}", card.getCardId());
            }
        }
        return true;
    }
}
