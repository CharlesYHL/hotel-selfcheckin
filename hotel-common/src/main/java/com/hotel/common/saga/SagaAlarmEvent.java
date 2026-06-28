package com.hotel.common.saga;

public record SagaAlarmEvent(SagaContext ctx, AlarmType type, String stepName) {}
