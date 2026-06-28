package com.hotel.common.saga;

public enum StepStatus {
    PENDING,
    EXECUTING,
    COMPLETED,
    COMPENSATED,
    FAILED
}
