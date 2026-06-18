package com.microservices.payment.event;

import com.microservices.payment.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    private String eventType;
    private Long paymentId;
    private String orderId;
    private Long userId;
    private BigDecimal amount;
    private PaymentStatus status;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
