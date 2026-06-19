package com.microservices.payment.merchant;

import com.microservices.payment.merchant.crypto.CryptoService;
import com.microservices.payment.merchant.entity.Merchant;
import com.microservices.payment.merchant.repository.MerchantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Seeds a deterministic sandbox merchant on first start so the merchant API can be
 * exercised immediately (key_id / key_secret come from config). Idempotent.
 */
@Slf4j
@Component
public class MerchantSeeder implements CommandLineRunner {

    private final MerchantRepository merchantRepository;
    private final CryptoService crypto;

    @Value("${app.merchant.seed-test-merchant:true}")
    private boolean seedEnabled;
    @Value("${app.merchant.test-key-id}")
    private String testKeyId;
    @Value("${app.merchant.test-key-secret}")
    private String testKeySecret;

    public MerchantSeeder(MerchantRepository merchantRepository, CryptoService crypto) {
        this.merchantRepository = merchantRepository;
        this.crypto = crypto;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }
        Boolean exists = merchantRepository.existsByKeyId(testKeyId).block();
        if (Boolean.TRUE.equals(exists)) {
            return;
        }

        Merchant merchant = Merchant.builder()
                .name("Sandbox Merchant")
                .email("sandbox@merchant.local")
                .keyId(testKeyId)
                .keySecretEnc(crypto.encrypt(testKeySecret))
                .active(true)
                .createdAt(Instant.now())
                .build();
        merchantRepository.save(merchant).block();
        log.warn("Seeded sandbox merchant key_id={} (test secret from config)", testKeyId);
    }
}
