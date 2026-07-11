package com.hotel.common.saga;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SagaRetryScheduler 单元测试")
class SagaRetrySchedulerTest {

    @Mock
    private SagaRepository repository;

    @Mock
    private SagaExecutor executor;

    @Mock
    private ApplicationContext applicationContext;

    private SagaRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SagaRetryScheduler(repository, executor, applicationContext);
    }

    @SagaStep(order = 1, name = "testStep")
    static class TestStep implements SagaStepInterface {
        @Override
        public Object execute(SagaContext context) {
            return "ok";
        }

        @Override
        public void compensate(SagaContext context) {
        }
    }

    private SagaContext createSagaContext(String sagaId, SagaStatus status, int retryCount) {
        SagaContext ctx = new SagaContext();
        ctx.setSagaId(sagaId);
        ctx.setOrderId("ORDER-001");
        ctx.setStatus(status);
        ctx.setRetryCount(retryCount);
        ctx.setUpdateTime(LocalDateTime.now());
        ctx.setStepRecords(new ArrayList<>());
        return ctx;
    }

    @Nested
    @DisplayName("卡住 Saga 检测")
    class StuckSagaDetection {

        @Test
        @DisplayName("检测到卡住的 Saga → 记录告警日志")
        void shouldLogStuckSagas() {
            SagaContext stuck = createSagaContext("saga-stuck", SagaStatus.RUNNING, 0);
            stuck.setUpdateTime(LocalDateTime.now().minusMinutes(15)); // 15 分钟前更新
            when(repository.findStuckSagas(any())).thenReturn(List.of(stuck));
            when(repository.findCompensationFailures(anyInt())).thenReturn(List.of());

            scheduler.retryFailedCompensations();

            verify(repository).findStuckSagas(any());
            verify(repository).findCompensationFailures(anyInt());
        }

        @Test
        @DisplayName("COMPENSATING 状态且超时 → 也被检测为卡住")
        void shouldDetectCompensatingStuckSagas() {
            SagaContext stuck = createSagaContext("saga-stuck-comp", SagaStatus.COMPENSATING, 0);
            stuck.setUpdateTime(LocalDateTime.now().minusMinutes(20));
            when(repository.findStuckSagas(any())).thenReturn(List.of(stuck));
            when(repository.findCompensationFailures(anyInt())).thenReturn(List.of());

            scheduler.retryFailedCompensations();

            verify(repository).findStuckSagas(any());
        }
    }

    @Nested
    @DisplayName("补偿重试")
    class CompensationRetry {

        @Test
        @DisplayName("retryCount < MAX_RETRIES → 执行补偿重试")
        void shouldRetryCompensationWhenUnderMaxRetries() {
            SagaContext ctx = createSagaContext("saga-retry", SagaStatus.COMPENSATED, 1);
            when(repository.findStuckSagas(any())).thenReturn(List.of());
            when(repository.findCompensationFailures(3)).thenReturn(List.of(ctx));
            when(applicationContext.getBean(TestStep.class)).thenReturn(new TestStep());

            scheduler.register("checkin_saga", List.of(TestStep.class));
            scheduler.retryFailedCompensations();

            // 验证 retryCount 递增
            assertEquals(2, ctx.getRetryCount());
            assertEquals(SagaStatus.COMPENSATING, ctx.getStatus());
            // 验证调用了补偿
            verify(executor).executeCompensate(anyList(), eq(ctx));
        }

        @Test
        @DisplayName("retryCount >= MAX_RETRIES → 不再重试，记录告警")
        void shouldNotRetryWhenMaxRetriesExhausted() {
            SagaContext ctx = createSagaContext("saga-exhausted", SagaStatus.COMPENSATED, 3);
            when(repository.findStuckSagas(any())).thenReturn(List.of());
            when(repository.findCompensationFailures(3)).thenReturn(List.of(ctx));

            scheduler.retryFailedCompensations();

            // 验证没有执行补偿（已耗尽重试次数）
            verify(executor, never()).executeCompensate(anyList(), any());
            // 验证状态未被修改
            assertEquals(3, ctx.getRetryCount());
        }

        @Test
        @DisplayName("retryCount 正好等于 MAX_RETRIES → 不重试")
        void shouldNotRetryWhenRetryCountEqualsMaxRetries() {
            SagaContext ctx = createSagaContext("saga-max", SagaStatus.COMPENSATED, 3);
            when(repository.findStuckSagas(any())).thenReturn(List.of());
            when(repository.findCompensationFailures(3)).thenReturn(List.of(ctx));

            scheduler.retryFailedCompensations();

            verify(executor, never()).executeCompensate(anyList(), any());
        }
    }

    @Nested
    @DisplayName("步骤注册表")
    class StepRegistry {

        @Test
        @DisplayName("未注册 sagaType → 记录警告，不抛异常")
        void shouldHandleMissingStepRegistry() {
            SagaContext ctx = createSagaContext("saga-no-reg", SagaStatus.COMPENSATED, 1);
            when(repository.findStuckSagas(any())).thenReturn(List.of());
            when(repository.findCompensationFailures(3)).thenReturn(List.of(ctx));

            // 不注册任何 sagaType
            assertDoesNotThrow(() -> scheduler.retryFailedCompensations());
            verify(executor, never()).executeCompensate(anyList(), any());
        }

        @Test
        @DisplayName("注册步骤后 → 正常重试补偿")
        void shouldRetryAfterRegistration() {
            SagaContext ctx = createSagaContext("saga-registered", SagaStatus.COMPENSATED, 0);
            when(repository.findStuckSagas(any())).thenReturn(List.of());
            when(repository.findCompensationFailures(3)).thenReturn(List.of(ctx));
            when(applicationContext.getBean(TestStep.class)).thenReturn(new TestStep());

            scheduler.register("checkin_saga", List.of(TestStep.class));
            scheduler.retryFailedCompensations();

            verify(executor).executeCompensate(anyList(), eq(ctx));
            assertEquals(1, ctx.getRetryCount());
        }
    }

    @Nested
    @DisplayName("批量处理")
    class BatchProcessing {

        @Test
        @DisplayName("多个 Saga 需要重试 → 逐个处理")
        void shouldProcessMultipleSagas() {
            SagaContext ctx1 = createSagaContext("saga-1", SagaStatus.COMPENSATED, 0);
            SagaContext ctx2 = createSagaContext("saga-2", SagaStatus.COMPENSATED, 1);
            when(repository.findStuckSagas(any())).thenReturn(List.of());
            when(repository.findCompensationFailures(3)).thenReturn(List.of(ctx1, ctx2));
            when(applicationContext.getBean(TestStep.class)).thenReturn(new TestStep());

            scheduler.register("checkin_saga", List.of(TestStep.class));
            scheduler.retryFailedCompensations();

            verify(executor, times(2)).executeCompensate(anyList(), any(SagaContext.class));
            assertEquals(1, ctx1.getRetryCount());
            assertEquals(2, ctx2.getRetryCount());
        }

        @Test
        @DisplayName("混合场景：一个重试，一个耗尽 → 一个重试，一个跳过")
        void shouldHandleMixedRetryScenarios() {
            SagaContext retryCtx = createSagaContext("saga-retry", SagaStatus.COMPENSATED, 1);
            SagaContext exhaustedCtx = createSagaContext("saga-exhausted", SagaStatus.COMPENSATED, 3);
            when(repository.findStuckSagas(any())).thenReturn(List.of());
            when(repository.findCompensationFailures(3)).thenReturn(List.of(retryCtx, exhaustedCtx));
            when(applicationContext.getBean(TestStep.class)).thenReturn(new TestStep());

            scheduler.register("checkin_saga", List.of(TestStep.class));
            scheduler.retryFailedCompensations();

            // 只重试了 saga-retry，saga-exhausted 被跳过
            verify(executor).executeCompensate(anyList(), eq(retryCtx));
            verify(executor, never()).executeCompensate(anyList(), eq(exhaustedCtx));
            assertEquals(2, retryCtx.getRetryCount());
            assertEquals(3, exhaustedCtx.getRetryCount());
        }
    }
}