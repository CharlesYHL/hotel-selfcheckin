package com.hotel.common.saga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaRetryScheduler {

    private final SagaRepository repository;
    private final SagaExecutor executor;
    private final ApplicationContext applicationContext;

    private static final int MAX_RETRIES = 3;
    private static final Duration STUCK_THRESHOLD = Duration.ofMinutes(10);

    private final Map<String, List<Class<? extends SagaStepInterface>>> stepRegistry = new ConcurrentHashMap<>();

    public void register(String sagaType, List<Class<? extends SagaStepInterface>> stepClasses) {
        stepRegistry.put(sagaType, stepClasses);
        log.info("注册 Saga 步骤定义: sagaType={}, steps={}", sagaType, stepClasses.size());
    }

    @Scheduled(fixedRate = 60000)
    public void retryFailedCompensations() {
        List<SagaContext> stuck = repository.findStuckSagas(STUCK_THRESHOLD);
        for (SagaContext ctx : stuck) {
            log.warn("检测到卡住 Saga: sagaId={}, status={}, elapsed={}min",
                    ctx.getSagaId(), ctx.getStatus(),
                    Duration.between(ctx.getUpdateTime(), LocalDateTime.now()).toMinutes());
        }

        List<SagaContext> failures = repository.findCompensationFailures(MAX_RETRIES);
        for (SagaContext ctx : failures) {
            if (ctx.getRetryCount() < MAX_RETRIES) {
                log.info("开始重试 Saga 补偿: sagaId={}, retry={}", ctx.getSagaId(), ctx.getRetryCount());
                retryCompensation(ctx);
            } else {
                log.error("Saga 补偿重试次数耗尽，需人工介入: sagaId={}", ctx.getSagaId());
            }
        }
    }

    private void retryCompensation(SagaContext ctx) {
        List<Class<? extends SagaStepInterface>> stepClasses = stepRegistry.get("checkin_saga");
        if (stepClasses == null) {
            log.warn("未找到 Saga 步骤定义，无法重试: sagaType=checkin_saga, sagaId={}", ctx.getSagaId());
            return;
        }

        List<SagaStepInterface> steps = stepClasses.stream()
                .map(applicationContext::getBean)
                .map(SagaStepInterface.class::cast)
                .toList();

        ctx.setRetryCount(ctx.getRetryCount() + 1);
        ctx.setStatus(SagaStatus.COMPENSATING);
        ctx.setUpdateTime(LocalDateTime.now());
        repository.save(ctx);

        executor.executeCompensate(steps, ctx);

        if (ctx.getStatus() == SagaStatus.COMPENSATED) {
            log.info("Saga 补偿重试成功: sagaId={}, retry={}", ctx.getSagaId(), ctx.getRetryCount());
        } else {
            log.error("Saga 补偿重试仍失败: sagaId={}, retry={}", ctx.getSagaId(), ctx.getRetryCount());
        }
    }
}
