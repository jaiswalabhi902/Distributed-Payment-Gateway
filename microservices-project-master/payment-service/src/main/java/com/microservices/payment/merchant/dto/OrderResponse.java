package com.microservices.payment.merchant.dto;

import com.microservices.payment.merchant.domain.OrderStatus;
import com.microservices.payment.merchant.entity.MerchantOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    /** Public order id (order_xxx). */
    private String id;
    private Long amount;
    private Long amountPaid;
    private String currency;
    private String receipt;
    private OrderStatus status;
    private String notes;
    private Instant createdAt;

    public static OrderResponse from(MerchantOrder o) {
        return OrderResponse.builder()
                .id(o.getOrderRef())
                .amount(o.getAmount())
                .amountPaid(o.getStatus() == OrderStatus.PAID ? o.getAmount() : 0L)
                .currency(o.getCurrency())
                .receipt(o.getReceipt())
                .status(o.getStatus())
                .notes(o.getNotes())
                .createdAt(o.getCreatedAt())
                .build();
    }
}
