package com.microservices.payment.merchant.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {

    /** Amount in the smallest currency unit (paise). Minimum ₹1.00 = 100. */
    @NotNull(message = "amount is required")
    @Min(value = 100, message = "amount must be at least 100 (₹1.00)")
    private Long amount;

    /** ISO currency code; defaults to INR. */
    private String currency = "INR";

    /** Merchant-side receipt / reference number. */
    private String receipt;

    /** Optional free-form notes. */
    private String notes;
}
