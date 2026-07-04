package com.hotel.common.saga;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SagaExecutor 单元测试")
class SagaExecutorTest {

    @Mock
    private SagaRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SagaExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new SagaExecutor(repository, eventPublisher);
    }

    // ========== Test Step Implementations ==========

    @SagaStep(order = 1, name = "step1")
    static class SuccessStep1 implements SagaStepInterface {
        private final AtomicInteger executeCount = new AtomicInteger(0);
        private final AtomicInteger compensateCount = new AtomicInteger(0);

        @Override
        public Object execute(SagaContext context) {
            executeCount.incrementAndGet();
            context.setPaymentId("PAY-001");
            return "step1-ok";
        }

        @Override
        public void compensate(SagaContext context) {
            compensateCount.incrementAndGet();
        }
    }

    @SagaStep(order = 2, name = "step2")
    static class SuccessStep2 implements SagaStepInterface {
        private final AtomicInteger executeCount = new AtomicInteger(0);
        private final AtomicInteger compensateCount = new AtomicInteger(0);

        @Override
        public Object execute(SagaContext context) {
            executeCount.incrementAndGet();
            context.setRoomId("ROOM-301");
            return "step2-ok";
        }

        @Override
        public void compensate(SagaContext context) {
            compensateCount.incrementAndGet();
        }
    }

    @SagaStep(order = 3, name = "step3")
    static class SuccessStep3 implements SagaStepInterface {
        private final AtomicInteger executeCount = new AtomicInteger(0);
        private final AtomicInteger compensateCount = new AtomicInteger(0);

        @Override
        public Object execute(SagaContext context) {
            executeCount.incrementAndGet();
            context.setCardId("CARD-001");
            return "step3-ok";
        }

        @Override
        public void compensate(SagaContext context) {
            compensateCount.incrementAndGet();
        }
    }

    @SagaStep(order = 3, name = "failingStep3")
    static class FailingStep3 implements SagaStepInterface {
        @Override
        public Object execute(SagaContext context) {
            throw new RuntimeException("第三步失败");
        }

        @Override
        public void compensate(SagaContext context) {
        }
    }

    @SagaStep(order = 2, name = "failingStep2")
    static class FailingStep2 implements SagaStepInterface {
        private final AtomicInteger compensateCount = new AtomicInteger(0);

        @Override
        public Object execute(SagaContext context) {
            throw new RuntimeException("第二步失败");
        }

        @Override
        public void compensate(SagaContext context) {
            compensateCount.incrementAndGet();
        }
    }

    @SagaStep(order = 1, name = "compensateFailingStep")
    static class CompensateFailingStep implements SagaStepInterface {
        @Override
        public Object execute(SagaContext context) {
            return "ok";
        }

        @Override
        public void compensate(SagaContext context) {
            throw new RuntimeException("补偿操作失败");
        }
    }

    @SagaStep(order = 1, name = "continueStep", continueOnFailure = true)
    static class ContinueOnFailureStep implements SagaStepInterface {
        @Override
        public Object execute(SagaContext context) {
            throw new RuntimeException("非关键步骤失败");
        }

        @Override
        public void compensate(SagaContext context) {
        }
    }

    @SagaStep(order = 2, name = "nextStep")
    static class NextStep implements SagaStepInterface {
        private final AtomicInteger executeCount = new AtomicInteger(0);

        @Override
        public Object execute(SagaContext context) {
            executeCount.incrementAndGet();
            return "next-ok";
        }

        @Override
        public void compensate(SagaContext context) {
        }
    }

    @Nested
    @DisplayName("正向执行 — 全部成功")
    class AllSuccess {

        @Test
        @DisplayName("三步全部成功 → COMPLETED → stepRecords 全部 COMPLETED")
        void shouldCompleteAllSteps() {
            SuccessStep1 step1 = new SuccessStep1();
            SuccessStep2 step2 = new SuccessStep2();
            SuccessStep3 step3 = new SuccessStep3();
            SagaContext ctx = new SagaContext();

            boolean result = executor.execute(List.of(step1, step2, step3), ctx);

            assertTrue(result);
            assertEquals(SagaStatus.COMPLETED, ctx.getStatus());
            assertEquals(3, ctx.getStepRecords().size());
            assertEquals("PAY-001", ctx.getPaymentId());
            assertEquals("ROOM-301", ctx.getRoomId());
            assertEquals("CARD-001", ctx.getCardId());

            // 验证每步状态
            for (SagaStepRecord record : ctx.getStepRecords()) {
                assertEquals(StepStatus.COMPLETED, record.getStatus());
            }
        }

        @Test
        @DisplayName("步骤按 order 排序执行")
        void shouldExecuteInOrder() {
            SuccessStep1 step1 = new SuccessStep1();
            SuccessStep2 step2 = new SuccessStep2();
            SuccessStep3 step3 = new SuccessStep3();
            SagaContext ctx = new SagaContext();

            executor.execute(List.of(step3, step1, step2), ctx); // 乱序传入

            List<SagaStepRecord> records = ctx.getStepRecords();
            assertEquals("step1", records.get(0).getStepName());
            assertEquals("step2", records.get(1).getStepName());
            assertEquals("step3", records.get(2).getStepName());
        }
    }

    @Nested
    @DisplayName("失败补偿")
    class Compensation {

        @Test
        @DisplayName("第二步失败 → 补偿第一步 → COMPENSATED")
        void shouldCompensateOnFailure() {
            SuccessStep1 step1 = new SuccessStep1();
            FailingStep2 step2 = new FailingStep2();
            SuccessStep3 step3 = new SuccessStep3();
            SagaContext ctx = new SagaContext();

            boolean result = executor.execute(List.of(step1, step2, step3), ctx);

            assertFalse(result);
            assertEquals(SagaStatus.COMPENSATED, ctx.getStatus());
            assertEquals(1, step1.compensateCount.get()); // step1 被补偿
            assertEquals(0, step3.executeCount.get());    // step3 未执行
            assertTrue(ctx.getErrorMessage().contains("失败"));
        }

        @Test
        @DisplayName("补偿逆序执行（先补偿后完成的步骤）")
        void shouldCompensateInReverseOrder() throws Exception {
            SuccessStep1 step1 = new SuccessStep1();
            SuccessStep2 step2 = new SuccessStep2();
            FailingStep3 step3 = new FailingStep3();
            SagaContext ctx = new SagaContext();

            List<String> compensateOrder = new ArrayList<>();

            // 使用 spy 记录补偿顺序
            SagaStepInterface spy1 = spy(step1);
            SagaStepInterface spy2 = spy(step2);

            executor.execute(List.of(spy1, spy2, step3), ctx);

            // 验证补偿被调用
            verify(spy2).compensate(ctx); // step2 先补偿
            verify(spy1).compensate(ctx); // step1 后补偿
        }

        @Test
        @DisplayName("补偿失败 → retryCount++ → 发布告警事件")
        void shouldHandleCompensationFailure() {
            CompensateFailingStep step1 = new CompensateFailingStep();
            FailingStep2 step2 = new FailingStep2();
            SagaContext ctx = new SagaContext();

            executor.execute(List.of(step1, step2), ctx);

            assertEquals(SagaStatus.COMPENSATED, ctx.getStatus());
            assertTrue(ctx.getRetryCount() > 0);
            // 告警事件被发布
            verify(eventPublisher, atLeastOnce()).publishEvent(any(SagaAlarmEvent.class));
        }
    }

    @Nested
    @DisplayName("continueOnFailure")
    class ContinueOnFailure {

        @Test
        @DisplayName("continueOnFailure=true → 失败后继续执行后续步骤")
        void shouldContinueOnFailure() {
            ContinueOnFailureStep step1 = new ContinueOnFailureStep();
            NextStep step2 = new NextStep();
            SagaContext ctx = new SagaContext();

            boolean result = executor.execute(List.of(step1, step2), ctx);

            assertTrue(result); // 因为 continueOnFailure，最终成功
            assertEquals(SagaStatus.COMPLETED, ctx.getStatus());
            assertEquals(1, step2.executeCount.get()); // step2 被执行了
        }
    }

    @Nested
    @DisplayName("SagaContext 生命周期")
    class ContextLifecycle {

        @Test
        @DisplayName("sagaId 为空时自动生成")
        void shouldAutoGenerateSagaId() {
            SuccessStep1 step1 = new SuccessStep1();
            SagaContext ctx = new SagaContext();
            assertNull(ctx.getSagaId());

            executor.execute(List.of(step1), ctx);

            assertNotNull(ctx.getSagaId());
        }

        @Test
        @DisplayName("保留已有的 sagaId")
        void shouldPreserveExistingSagaId() {
            SuccessStep1 step1 = new SuccessStep1();
            SagaContext ctx = new SagaContext();
            ctx.setSagaId("existing-saga-id");

            executor.execute(List.of(step1), ctx);

            assertEquals("existing-saga-id", ctx.getSagaId());
        }
    }
}
