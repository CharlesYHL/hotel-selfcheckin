package com.hotel.common.saga;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

@Data
public class SagaContext implements Serializable {
    private String sagaId;
    private String orderId;
    private String roomId;
    private String cardId;
    private String paymentId;
    private String checkInId;
    private Map<String, Object> attributes = new HashMap<>();
    private List<SagaStepRecord> stepRecords = new ArrayList<>();
    private SagaStatus status = SagaStatus.RUNNING;
    private int currentStep = 0;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime updateTime;
    private LocalDateTime endTime;
    private LocalDateTime globalDeadline;
    private int retryCount = 0;
}
