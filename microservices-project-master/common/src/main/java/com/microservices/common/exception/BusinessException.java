package com.microservices.common.exception;

/**
 * Thrown when a business rule is violated. Maps to HTTP 400/409 depending on context.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
