package com.hotel.common.saga;

public interface SagaStepInterface {
    Object execute(SagaContext context) throws Exception;
    void compensate(SagaContext context) throws Exception;
}
