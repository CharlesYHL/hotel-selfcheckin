package com.hotel.common.saga;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SagaStepRecord implements Serializable {
    private int stepIndex;
    private String stepName;
    private StepStatus status;
    private String forwardResult;
    private String compensateResult;
    private String errorMessage;
    private LocalDateTime executeTime;
    private LocalDateTime compensateTime;
}
