package com.microservices.payment.merchant.service;

import com.microservices.common.exception.BusinessException;
import com.microservices.payment.merchant.crypto.CryptoService;
import com.microservices.payment.merchant.dto.CreateMerchantRequest;
import com.microservices.payment.merchant.dto.MerchantResponse;
import com.microservices.payment.merchant.entity.Merchant;
import com.microservices.payment.merchant.repository.MerchantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final CryptoService crypto;

    public MerchantService(MerchantRepository merchantRepository, CryptoService crypto) {
        this.merchantRepository = merchantRepository;
        this.crypto = crypto;
    }

    /** Creates a merchant and returns the raw key secret exactly once. */
    public Mono<MerchantResponse> create(CreateMerchantRequest request) {
        return merchantRepository.existsByEmail(request.getEmail())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.error(new BusinessException(
                                "A merchant with this email already exists"));
                    }
                    String keyId = "mk_test_" + CryptoService.randomToken(18);
                    String keySecret = CryptoService.randomToken(32);

                    Merchant merchant = Merchant.builder()
                            .name(request.getName())
                            .email(request.getEmail())
                            .keyId(keyId)
                            .keySecretEnc(crypto.encrypt(keySecret))
                            .webhookUrl(request.getWebhookUrl())
                            .active(true)
                            .createdAt(Instant.now())
                            .build();

                    return merchantRepository.save(merchant)
                            .doOnSuccess(m -> log.info("Created merchant {} ({})",
                                    m.getId(), m.getKeyId()))
                            .map(m -> toResponse(m, keySecret));
                });
    }

    public Flux<MerchantResponse> list() {
        return merchantRepository.findAll().map(m -> toResponse(m, null));
    }

    /** Validates Basic-auth credentials and returns the active merchant. */
    public Mono<Merchant> authenticate(String keyId, String rawSecret) {
        return merchantRepository.findByKeyId(keyId)
                .filter(Merchant::isActive)
                .filter(m -> {
                    String stored = crypto.decrypt(m.getKeySecretEnc());
                    return crypto.constantTimeEquals(stored, rawSecret);
                });
    }

    public String decryptSecret(Merchant merchant) {
        return crypto.decrypt(merchant.getKeySecretEnc());
    }

    private MerchantResponse toResponse(Merchant m, String rawSecret) {
        return MerchantResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .email(m.getEmail())
                .keyId(m.getKeyId())
                .keySecret(rawSecret)
                .webhookUrl(m.getWebhookUrl())
                .active(m.isActive())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
