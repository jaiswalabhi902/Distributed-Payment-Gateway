package com.microservices.payment.merchant.domain;

public enum OrderStatus {
    /** Order created, awaiting payment. */
    CREATED,
    /** A payment was attempted against the order. */
    ATTEMPTED,
    /** Order fully paid (captured). */
    PAID,
    /** Payment failed. */
    FAILED
}
