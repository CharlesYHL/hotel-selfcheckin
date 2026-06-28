package com.hotel.common.idempotent;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    String keyPrefix() default "";

    String staticKey() default "";

    String[] fields() default {};

    long expireSeconds() default 86400;

    long timeoutSeconds() default 0;

    boolean deleteOnSuccess() default false;

    ConcurrentStrategy concurrentStrategy() default ConcurrentStrategy.WAIT;

    enum ConcurrentStrategy {
        WAIT,
        THROW,
        SKIP
    }
}
