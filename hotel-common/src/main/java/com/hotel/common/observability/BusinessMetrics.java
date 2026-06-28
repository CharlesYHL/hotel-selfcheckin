package com.hotel.common.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordOrderCreated(String source) {
        meterRegistry.counter("hotel.order.created", "source", source).increment();
    }

    public void recordOrderPaid(String source) {
        meterRegistry.counter("hotel.order.paid", "source", source).increment();
    }

    public void recordOrderFailed(String source, String reason) {
        meterRegistry.counter("hotel.order.failed", "source", source, "reason", reason).increment();
    }

    public void recordCheckInSuccess() {
        meterRegistry.counter("hotel.checkin.success").increment();
    }

    public void recordCheckInFailed(String reason) {
        meterRegistry.counter("hotel.checkin.failed", "reason", reason).increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordDuration(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("hotel.operation.duration")
                .tag("operation", operation)
                .register(meterRegistry));
    }

    public void recordSagaCompleted() {
        meterRegistry.counter("hotel.saga.completed").increment();
    }

    public void recordSagaFailed(String reason) {
        meterRegistry.counter("hotel.saga.failed", "reason", reason).increment();
    }

    public void recordInventoryDecrement(String hotelId) {
        meterRegistry.counter("hotel.inventory.decrement", "hotelId", hotelId).increment();
    }

    public void recordPaymentCallback(String channel) {
        meterRegistry.counter("hotel.payment.callback", "channel", channel).increment();
    }
}
