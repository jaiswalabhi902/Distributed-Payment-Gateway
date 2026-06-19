package com.microservices.payment.merchant.entity;

import com.microservices.payment.merchant.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("merchant_orders")
public class MerchantOrder {

    @Id
    private Long id;

    /** Public order id returned to the merchant. e.g. order_xxx */
    @Column("order_ref")
    private String orderRef;

    @Column("merchant_id")
    private Long merchantId;

    /** Amount in the smallest currency unit (paise for INR). */
    private Long amount;

    private String currency;

    private String receipt;

    private OrderStatus status;

    /** Optional free-form merchant notes (stored as text/JSON). */
    private String notes;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
