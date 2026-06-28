package com.hotel.checkin.saga;

import com.hotel.checkin.model.entity.CheckIn;
import com.hotel.checkin.model.enums.CheckInStatus;
import com.hotel.checkin.repository.CheckInMapper;
import com.hotel.common.saga.SagaStepInterface;
import com.hotel.common.saga.SagaContext;
import com.hotel.common.saga.SagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Saga 步骤：入住服务。
 * 在分布式事务编排中被调用，支持 execute / compensate。
 */
@Component
@Slf4j
@RequiredArgsConstructor
@SagaStep(order = 3, name = "checkInStep", timeoutSeconds = 30)
public class CheckInSagaStep implements SagaStepInterface {

    private final CheckInMapper checkInMapper;

    @Override
    public Object execute(SagaContext context) {
        String checkinId = (String) context.getAttributes().get("checkinId");
        if (checkinId == null) {
            log.warn("Saga 上下文中无 checkinId，跳过");
            return true;
        }
        log.info("Saga 执行: checkInStep, checkinId={}", checkinId);
        return true;
    }

    @Override
    public void compensate(SagaContext context) {
        String checkinId = (String) context.getAttributes().get("checkinId");
        if (checkinId == null) {
            return;
        }
        CheckIn checkIn = checkInMapper.selectById(checkinId);
        if (checkIn == null) {
            return;
        }
        checkInMapper.updateStatus(checkIn.getCheckinId(),
                CheckInStatus.NO_SHOW.getCode(), checkIn.getStatus());
        log.info("Saga 补偿: 取消入住, checkinId={}", checkinId);
    }
}
