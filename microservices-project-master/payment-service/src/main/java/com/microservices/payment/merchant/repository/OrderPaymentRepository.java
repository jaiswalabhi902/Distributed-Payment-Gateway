package com.microservices.payment.merchant.repository;

import com.microservices.payment.merchant.entity.OrderPayment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface OrderPaymentRepository extends ReactiveCrudRepository<OrderPayment, Long> {

    Mono<OrderPayment> findByPaymentRef(String paymentRef);
}
