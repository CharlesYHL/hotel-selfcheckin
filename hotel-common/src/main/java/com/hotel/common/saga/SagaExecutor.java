package com.hotel.common.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaExecutor {

    private final SagaRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public <T extends SagaStepInterface> boolean execute(List<T> steps, SagaContext context) {
        if (context.getSagaId() == null) {
            context.setSagaId(UUID.randomUUID().toString());
        }
        context.setStartTime(LocalDateTime.now());
        context.setUpdateTime(LocalDateTime.now());
        context.setStatus(SagaStatus.RUNNING);
        repository.save(context);

        List<T> sorted = steps.stream()
                .sorted(Comparator.comparingInt(s -> s.getClass().getAnnotation(SagaStep.class).order()))
                .toList();

        boolean ok = executeForward(sorted, context);
        if (ok) {
            context.setStatus(SagaStatus.COMPLETED);
            context.setEndTime(LocalDateTime.now());
            repository.save(context);
            return true;
        }
        context.setStatus(SagaStatus.COMPENSATING);
        repository.save(context);
        executeCompensate(sorted, context);
        return false;
    }

    private <T extends SagaStepInterface> boolean executeForward(List<T> steps, SagaContext context) {
        for (int i = 0; i < steps.size(); i++) {
            T step = steps.get(i);
            SagaStep ann = step.getClass().getAnnotation(SagaStep.class);
            SagaStepRecord record = new SagaStepRecord();
            record.setStepIndex(i);
            record.setStepName(ann.name());
            record.setStatus(StepStatus.EXECUTING);
            record.setExecuteTime(LocalDateTime.now());

            try {
                log.info("Saga 执行: sagaId={}, step={}", context.getSagaId(), ann.name());
                Object result = step.execute(context);
                record.setStatus(StepStatus.COMPLETED);
                record.setForwardResult(serialize(result));
                context.getStepRecords().add(record);
                context.setCurrentStep(i + 1);
                context.setUpdateTime(LocalDateTime.now());
                repository.save(context);
            } catch (Exception e) {
                log.error("Saga 步骤失败: sagaId={}, step={}, error={}",
                        context.getSagaId(), ann.name(), e.getMessage());
                record.setStatus(StepStatus.FAILED);
                record.setErrorMessage(e.getMessage());
                context.getStepRecords().add(record);
                context.setStatus(SagaStatus.FAILED);
                context.setErrorMessage(ann.name() + " 失败: " + e.getMessage());
                context.setUpdateTime(LocalDateTime.now());
                repository.save(context);
                publishEvent(new SagaAlarmEvent(context, AlarmType.STEP_FAILED, ann.name()));
                if (!ann.continueOnFailure()) return false;
            }
        }
        return true;
    }

    private void publishEvent(SagaAlarmEvent event) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(event);
        }
    }

    public <T extends SagaStepInterface> void executeCompensate(List<T> steps, SagaContext context) {
        log.warn("Saga 开始补偿: sagaId={}", context.getSagaId());
        List<SagaStepRecord> completed = context.getStepRecords().stream()
                .filter(r -> r.getStatus() == StepStatus.COMPLETED)
                .sorted(Comparator.comparingInt(SagaStepRecord::getStepIndex).reversed())
                .toList();

        for (SagaStepRecord record : completed) {
            int idx = record.getStepIndex();
            if (idx >= steps.size()) continue;
            T step = steps.get(idx);
            SagaStep ann = step.getClass().getAnnotation(SagaStep.class);

            try {
                log.info("Saga 补偿: sagaId={}, step={}", context.getSagaId(), ann.name());
                step.compensate(context);
                record.setStatus(StepStatus.COMPENSATED);
                record.setCompensateTime(LocalDateTime.now());
            } catch (Exception e) {
                log.error("Saga 补偿失败（将进入重试队列）: sagaId={}, step={}",
                        context.getSagaId(), ann.name(), e);
                record.setErrorMessage("补偿异常: " + e.getMessage());
                context.setRetryCount(context.getRetryCount() + 1);
                publishEvent(new SagaAlarmEvent(context, AlarmType.COMPENSATE_FAILED, ann.name()));
            }
            context.setUpdateTime(LocalDateTime.now());
            repository.save(context);
        }
        context.setStatus(SagaStatus.COMPENSATED);
        context.setEndTime(LocalDateTime.now());
        repository.save(context);
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
