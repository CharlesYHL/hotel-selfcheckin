package com.hotel.room.saga;

import com.hotel.common.saga.SagaContext;
import com.hotel.common.saga.SagaStep;
import com.hotel.common.saga.SagaStepInterface;
import com.hotel.room.service.RoomAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@SagaStep(order = 2, name = "assignRoom", timeoutSeconds = 30)
@RequiredArgsConstructor
@Slf4j
public class RoomAssignStep implements SagaStepInterface {

    private final RoomAssignmentService assignmentService;

    @Override
    public Object execute(SagaContext context) {
        var result = assignmentService.assign(
                context.getOrderId(),
                context.getAttributes().get("hotelId").toString(),
                context.getAttributes().get("roomTypeId").toString());

        if (!result.success()) {
            throw new RuntimeException(result.message());
        }

        context.setRoomId(result.room().getRoomId());
        log.info("Saga 排房成功: sagaId={}, orderId={}, roomId={}",
                context.getSagaId(), context.getOrderId(), result.room().getRoomId());
        return result;
    }

    @Override
    public void compensate(SagaContext context) {
        if (context.getRoomId() != null) {
            log.info("Saga 补偿释放房间: sagaId={}, roomId={}", context.getSagaId(), context.getRoomId());
            assignmentService.release(context.getRoomId(), context.getOrderId());
        }
    }
}
