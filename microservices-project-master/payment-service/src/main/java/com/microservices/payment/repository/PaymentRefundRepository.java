package com.microservices.payment.repository;

import com.microservices.payment.entity.PaymentRefund;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PaymentRefundRepository extends ReactiveCrudRepository<PaymentRefund, Long> {

    Flux<PaymentRefund> findByPaymentId(Long paymentId);
}
