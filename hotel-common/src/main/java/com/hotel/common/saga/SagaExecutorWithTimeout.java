package com.hotel.common.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaExecutorWithTimeout {

    private final SagaRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Duration DEFAULT_STEP_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration SAGA_GLOBAL_TIMEOUT = Duration.ofMinutes(2);

    private final ExecutorService timeoutExecutor = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200),
            r -> new Thread(r, "saga-timeout"),
            new ThreadPoolExecutor.CallerRunsPolicy());

    @jakarta.annotation.PreDestroy
    public void shutdown() {
        timeoutExecutor.shutdown();
        try {
            if (!timeoutExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                timeoutExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            timeoutExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public <T extends SagaStepInterface> boolean execute(List<T> steps, SagaContext ctx) {
        if (ctx.getSagaId() == null) {
            ctx.setSagaId(UUID.randomUUID().toString());
        }
        ctx.setStartTime(LocalDateTime.now());
        ctx.setGlobalDeadline(ctx.getStartTime().plus(SAGA_GLOBAL_TIMEOUT));
        ctx.setStatus(SagaStatus.RUNNING);
        repository.save(ctx);

        List<T> sorted = steps.stream()
                .sorted(Comparator.comparingInt(s -> s.getClass().getAnnotation(SagaStep.class).order()))
                .toList();

        boolean ok = executeWithTimeout(sorted, ctx);
        if (!ok) {
            ctx.setStatus(SagaStatus.COMPENSATING);
            repository.save(ctx);
            SagaExecutor fallback = new SagaExecutor(repository, eventPublisher);
            fallback.executeCompensate(sorted, ctx);
        }
        return ok;
    }

    private <T extends SagaStepInterface> boolean executeWithTimeout(List<T> steps, SagaContext ctx) {
        for (T step : steps) {
            if (LocalDateTime.now().isAfter(ctx.getGlobalDeadline())) {
                log.error("Saga 全局超时，中止执行: sagaId={}", ctx.getSagaId());
                ctx.setErrorMessage("Saga 执行全局超时");
                ctx.setStatus(SagaStatus.FAILED);
                repository.save(ctx);
                return false;
            }

            SagaStep ann = step.getClass().getAnnotation(SagaStep.class);
            Duration stepTimeout = ann.timeoutSeconds() > 0
                    ? Duration.ofSeconds(ann.timeoutSeconds())
                    : DEFAULT_STEP_TIMEOUT;

            FutureTask<Boolean> task = new FutureTask<>(() -> {
                SagaStepRecord record = new SagaStepRecord();
                record.setStepIndex(ctx.getCurrentStep());
                record.setStepName(ann.name());
                record.setExecuteTime(LocalDateTime.now());
                record.setStatus(StepStatus.EXECUTING);

                try {
                    Object result = step.execute(ctx);
                    // 如果执行期间被中断（超时取消），不记录为完成
                    if (Thread.currentThread().isInterrupted()) {
                        return false;
                    }
                    record.setStatus(StepStatus.COMPLETED);
                    record.setForwardResult(serialize(result));
                    ctx.getStepRecords().add(record);
                    ctx.setCurrentStep(ctx.getCurrentStep() + 1);
                    ctx.setUpdateTime(LocalDateTime.now());
                    repository.save(ctx);
                    return true;
                } catch (Exception e) {
                    record.setStatus(StepStatus.FAILED);
                    record.setErrorMessage(e.getMessage());
                    ctx.getStepRecords().add(record);
                    ctx.setErrorMessage(ann.name() + " 异常: " + e.getMessage());
                    ctx.setStatus(SagaStatus.FAILED);
                    ctx.setUpdateTime(LocalDateTime.now());
                    repository.save(ctx);
                    return false;
                }
            });

            timeoutExecutor.submit(task);
            try {
                if (!task.get(stepTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    return false;
                }
            } catch (TimeoutException e) {
                task.cancel(true);
                log.error("Saga 步骤超时: sagaId={}, step={}, timeout={}s",
                        ctx.getSagaId(), ann.name(), stepTimeout.toSeconds());
                ctx.setErrorMessage("步骤超时: " + ann.name());
                ctx.setStatus(SagaStatus.FAILED);
                repository.save(ctx);
                return false;
            } catch (Exception e) {
                log.error("Saga 步骤异常: {}", ann.name(), e);
                ctx.setErrorMessage(ann.name() + " 异常: " + e.getMessage());
                ctx.setStatus(SagaStatus.FAILED);
                repository.save(ctx);
                return false;
            }
        }
        ctx.setStatus(SagaStatus.COMPLETED);
        ctx.setUpdateTime(LocalDateTime.now());
        repository.save(ctx);
        return true;
    }

    private String serialize(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }
}
