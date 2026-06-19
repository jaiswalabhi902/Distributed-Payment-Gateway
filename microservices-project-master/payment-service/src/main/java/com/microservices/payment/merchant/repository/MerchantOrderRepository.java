package com.microservices.payment.merchant.repository;

import com.microservices.payment.merchant.entity.MerchantOrder;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MerchantOrderRepository extends ReactiveCrudRepository<MerchantOrder, Long> {

    Mono<MerchantOrder> findByOrderRef(String orderRef);

    Flux<MerchantOrder> findByMerchantIdOrderByIdDesc(Long merchantId);
}
