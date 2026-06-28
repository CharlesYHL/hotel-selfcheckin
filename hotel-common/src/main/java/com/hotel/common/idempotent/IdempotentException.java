package com.hotel.common.idempotent;

public class IdempotentException extends RuntimeException {
    public IdempotentException(String message) {
        super(message);
    }
}
