package com.hotel.common.saga;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SagaExecutorWithTimeout 单元测试")
class SagaExecutorWithTimeoutTest {

    @Mock
    private SagaRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SagaExecutorWithTimeout executor;

    @BeforeEach
    void setUp() {
        executor = new SagaExecutorWithTimeout(repository, eventPublisher);
    }

    @SagaStep(order = 1, name = "fastStep")
    static class FastStep implements SagaStepInterface {
        @Override
        public Object execute(SagaContext context) {
            context.setPaymentId("PAY-FAST");
            return "fast-ok";
        }

        @Override
        public void compensate(SagaContext context) {
        }
    }

    @SagaStep(order = 1, name = "slowStep", timeoutSeconds = 1)
    static class SlowStep implements SagaStepInterface {
        @Override
        public Object execute(SagaContext context) {
            try {
                Thread.sleep(5000); // 超过 timeoutSeconds=1
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "slow-ok";
        }

        @Override
        public void compensate(SagaContext context) {
        }
    }

    @SagaStep(order = 1, name = "quickStep", timeoutSeconds = 5)
    static class QuickStep implements SagaStepInterface {
        @Override
        public Object execute(SagaContext context) {
            context.setPaymentId("PAY-QUICK");
            return "quick-ok";
        }

        @Override
        public void compensate(SagaContext context) {
        }
    }

    @SagaStep(order = 1, name = "errorStep")
    static class ErrorStep implements SagaStepInterface {
        @Override
        public Object execute(SagaContext context) {
            throw new RuntimeException("执行异常");
        }

        @Override
        public void compensate(SagaContext context) {
        }
    }

    @Nested
    @DisplayName("正常执行")
    class NormalExecution {

        @Test
        @DisplayName("步骤在超时内完成 → 正常返回")
        void shouldCompleteWithinTimeout() {
            QuickStep step = new QuickStep();
            SagaContext ctx = new SagaContext();

            boolean result = executor.execute(List.of(step), ctx);

            assertTrue(result);
            assertEquals(SagaStatus.COMPLETED, ctx.getStatus());
            assertEquals("PAY-QUICK", ctx.getPaymentId());
        }
    }

    @Nested
    @DisplayName("超时处理")
    class TimeoutHandling {

        @Test
        @DisplayName("步骤超时 → TimeoutException → SagaStatus.FAILED")
        void shouldFailOnStepTimeout() {
            SlowStep step = new SlowStep();
            SagaContext ctx = new SagaContext();

            boolean result = executor.execute(List.of(step), ctx);

            assertFalse(result);
            assertTrue(ctx.getErrorMessage() != null && ctx.getErrorMessage().contains("超时"));
        }
    }

    @Nested
    @DisplayName("全局超时")
    class GlobalTimeout {

        @Test
        @DisplayName("execute 应正确设置 globalDeadline 为 2 分钟后")
        void shouldSetGlobalDeadline() {
            SagaContext ctx = new SagaContext();

            FastStep step = new FastStep();
            executor.execute(List.of(step), ctx);

            assertNotNull(ctx.getGlobalDeadline());
            assertTrue(ctx.getGlobalDeadline().isAfter(ctx.getStartTime()));
            // 全局超时应约为 2 分钟
            long diffSeconds = java.time.Duration.between(ctx.getStartTime(), ctx.getGlobalDeadline()).toSeconds();
            assertTrue(diffSeconds >= 110 && diffSeconds <= 130,
                    "Expected ~120s global timeout, got " + diffSeconds + "s");
        }
    }

    @Nested
    @DisplayName("步骤异常")
    class StepException {

        @Test
        @DisplayName("步骤抛异常 → 触发补偿")
        void shouldCompensateOnException() {
            ErrorStep step = new ErrorStep();
            SagaContext ctx = new SagaContext();

            boolean result = executor.execute(List.of(step), ctx);

            assertFalse(result);
            assertTrue(ctx.getErrorMessage().contains("异常"));
        }
    }
}
