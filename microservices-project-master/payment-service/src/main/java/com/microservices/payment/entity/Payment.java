package com.microservices.payment.entity;

import com.microservices.payment.domain.PaymentMethod;
import com.microservices.payment.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("payments")
public class Payment {

    @Id
    private Long id;

    @Column("order_id")
    private String orderId;

    @Column("user_id")
    private Long userId;

    private BigDecimal amount;

    private String currency;

    private PaymentStatus status;

    @Column("payment_method")
    private PaymentMethod paymentMethod;

    @Column("transaction_id")
    private String transactionId;

    private String description;

    @Column("refunded_amount")
    private BigDecimal refundedAmount;

    @Version
    private Long version;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
