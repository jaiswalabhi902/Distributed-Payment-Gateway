package com.microservices.payment.domain;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    REFUNDED,
    PARTIAL_REFUND
}
