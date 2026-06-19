package com.microservices.payment.merchant.repository;

import com.microservices.payment.merchant.entity.Merchant;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface MerchantRepository extends ReactiveCrudRepository<Merchant, Long> {

    Mono<Merchant> findByKeyId(String keyId);

    Mono<Boolean> existsByKeyId(String keyId);

    Mono<Boolean> existsByEmail(String email);
}
