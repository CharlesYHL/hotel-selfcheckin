package com.hotel.common.saga;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SagaStep {
    int order();
    String name();
    boolean continueOnFailure() default false;
    int timeoutSeconds() default 0;
}
