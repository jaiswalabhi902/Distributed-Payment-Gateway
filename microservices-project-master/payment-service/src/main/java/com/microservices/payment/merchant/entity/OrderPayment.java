package com.microservices.payment.merchant.entity;

import com.microservices.payment.domain.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/** A capture attempt against a merchant order (one order can have several attempts). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("merchant_order_payments")
public class OrderPayment {

    @Id
    private Long id;

    /** Public payment id returned to the merchant. e.g. pay_xxx */
    @Column("payment_ref")
    private String paymentRef;

    @Column("order_ref")
    private String orderRef;

    @Column("merchant_id")
    private Long merchantId;

    private Long amount;

    private String currency;

    private PaymentMethod method;

    /** CAPTURED or FAILED. */
    private String status;

    @Column("created_at")
    private Instant createdAt;
}
