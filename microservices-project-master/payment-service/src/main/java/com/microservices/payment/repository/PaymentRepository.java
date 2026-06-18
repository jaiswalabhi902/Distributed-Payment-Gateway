package com.microservices.payment.repository;

import com.microservices.payment.domain.PaymentStatus;
import com.microservices.payment.entity.Payment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentRepository extends ReactiveCrudRepository<Payment, Long> {

    Flux<Payment> findByUserId(Long userId);

    Flux<Payment> findByStatus(PaymentStatus status);

    Mono<Payment> findByOrderId(String orderId);

    Mono<Long> countByUserId(Long userId);

    Mono<Boolean> existsByOrderId(String orderId);
}
