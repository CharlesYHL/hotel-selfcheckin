package com.hotel.common.saga;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SagaAlarmHandler {

    @EventListener
    public void handle(SagaAlarmEvent event) {
        String title = "[Saga 告警] " + event.ctx().getSagaId();
        String msg = switch (event.type()) {
            case STEP_FAILED -> "步骤执行失败: " + event.stepName() + ", 错误: " + event.ctx().getErrorMessage();
            case COMPENSATE_FAILED -> "补偿失败: " + event.stepName() + ", 已重试: " + event.ctx().getRetryCount() + " 次";
            case STUCK -> "Saga 卡住超过 10 分钟，当前状态: " + event.ctx().getStatus();
            case EXHAUSTED -> "Saga 补偿重试次数耗尽，请人工介入！";
        };
        log.error("{} - {}", title, msg);
    }
}
