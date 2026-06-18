package com.microservices.payment.service;

import com.microservices.common.exception.BusinessException;
import com.microservices.common.exception.ResourceNotFoundException;
import com.microservices.payment.domain.PaymentStatus;
import com.microservices.payment.dto.CreatePaymentRequest;
import com.microservices.payment.dto.PaymentResponse;
import com.microservices.payment.dto.RefundRequest;
import com.microservices.payment.entity.Payment;
import com.microservices.payment.entity.PaymentRefund;
import com.microservices.payment.event.PaymentEventPublisher;
import com.microservices.payment.repository.PaymentRefundRepository;
import com.microservices.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository refundRepository;
    private final PaymentEventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentRefundRepository refundRepository,
                          PaymentEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.eventPublisher = eventPublisher;
    }

    public Mono<PaymentResponse> createPayment(Long userId, CreatePaymentRequest request) {
        return paymentRepository.existsByOrderId(request.getOrderId())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Mono.error(new BusinessException(
                                "Payment already exists for order " + request.getOrderId()));
                    }
                    Instant now = Instant.now();
                    Payment payment = Payment.builder()
                            .orderId(request.getOrderId())
                            .userId(userId)
                            .amount(request.getAmount())
                            .currency(request.getCurrency().toUpperCase())
                            .status(PaymentStatus.PENDING)
                            .paymentMethod(request.getPaymentMethod())
                            .transactionId(generateTransactionId())
                            .description(request.getDescription())
                            .refundedAmount(BigDecimal.ZERO)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    return paymentRepository.save(payment)
                            .doOnSuccess(eventPublisher::publishCreated)
                            .doOnSuccess(p -> log.info("Created payment {} for user {}",
                                    p.getId(), userId));
                })
                .map(PaymentResponse::from);
    }

    public Mono<PaymentResponse> getPayment(Long id) {
        return paymentRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Payment", id)))
                .map(PaymentResponse::from);
    }

    public Flux<PaymentResponse> getUserPayments(Long userId) {
        return paymentRepository.findByUserId(userId).map(PaymentResponse::from);
    }

    public Flux<PaymentResponse> getByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status).map(PaymentResponse::from);
    }

    public Mono<Long> countUserPayments(Long userId) {
        return paymentRepository.countByUserId(userId);
    }

    public Mono<PaymentResponse> updateStatus(Long id, PaymentStatus status, String reason) {
        return paymentRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Payment", id)))
                .flatMap(payment -> {
                    payment.setStatus(status);
                    payment.setUpdatedAt(Instant.now());
                    if (reason != null && !reason.isBlank()) {
                        payment.setDescription(reason);
                    }
                    return paymentRepository.save(payment)
                            .doOnSuccess(eventPublisher::publishUpdated);
                })
                .map(PaymentResponse::from);
    }

    public Mono<PaymentResponse> refund(Long id, RefundRequest request) {
        return paymentRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Payment", id)))
                .flatMap(payment -> validateAndApplyRefund(payment, request));
    }

    private Mono<PaymentResponse> validateAndApplyRefund(Payment payment, RefundRequest request) {
        if (payment.getStatus() == PaymentStatus.PENDING
                || payment.getStatus() == PaymentStatus.FAILED) {
            return Mono.error(new BusinessException(
                    "Cannot refund a payment in status " + payment.getStatus()));
        }

        BigDecimal alreadyRefunded = payment.getRefundedAmount() == null
                ? BigDecimal.ZERO : payment.getRefundedAmount();
        BigDecimal remaining = payment.getAmount().subtract(alreadyRefunded);
        if (request.getRefundAmount().compareTo(remaining) > 0) {
            return Mono.error(new BusinessException(
                    "Refund amount exceeds refundable balance of " + remaining));
        }

        BigDecimal newRefunded = alreadyRefunded.add(request.getRefundAmount());
        payment.setRefundedAmount(newRefunded);
        payment.setStatus(newRefunded.compareTo(payment.getAmount()) >= 0
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIAL_REFUND);
        payment.setUpdatedAt(Instant.now());

        PaymentRefund refund = PaymentRefund.builder()
                .paymentId(payment.getId())
                .refundAmount(request.getRefundAmount())
                .reason(request.getRefundReason())
                .status("COMPLETED")
                .createdAt(Instant.now())
                .build();

        return refundRepository.save(refund)
                .then(paymentRepository.save(payment))
                .doOnSuccess(eventPublisher::publishRefunded)
                .doOnSuccess(p -> log.info("Refunded {} on payment {}",
                        request.getRefundAmount(), p.getId()))
                .map(PaymentResponse::from);
    }

    private String generateTransactionId() {
        return "TXN-" + System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
