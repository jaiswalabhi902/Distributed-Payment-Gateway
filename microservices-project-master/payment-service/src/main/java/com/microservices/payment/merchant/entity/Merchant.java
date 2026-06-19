package com.microservices.payment.merchant.entity;

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
@Table("merchants")
public class Merchant {

    @Id
    private Long id;

    private String name;

    private String email;

    /** Public, used in checkout and the Basic auth username. e.g. mk_test_xxx */
    @Column("key_id")
    private String keyId;

    /** AES-GCM encrypted key secret (raw secret needed for HMAC signing). */
    @Column("key_secret_enc")
    private String keySecretEnc;

    @Column("webhook_url")
    private String webhookUrl;

    private boolean active;

    @Column("created_at")
    private Instant createdAt;
}
