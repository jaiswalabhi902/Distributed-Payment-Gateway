package com.microservices.payment.dto;

import com.microservices.payment.domain.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private PaymentStatus status;

    private String reason;
}
