package com.microservices.payment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("payment_refunds")
public class PaymentRefund {

    @Id
    private Long id;

    @Column("payment_id")
    private Long paymentId;

    @Column("refund_amount")
    private BigDecimal refundAmount;

    private String reason;

    private String status;

    @Column("created_at")
    private Instant createdAt;
}
