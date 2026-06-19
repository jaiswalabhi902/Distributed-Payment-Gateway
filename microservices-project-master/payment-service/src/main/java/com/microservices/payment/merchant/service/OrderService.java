package com.microservices.payment.merchant.service;

import com.microservices.common.exception.BusinessException;
import com.microservices.common.exception.ResourceNotFoundException;
import com.microservices.payment.merchant.crypto.CryptoService;
import com.microservices.payment.merchant.domain.OrderStatus;
import com.microservices.payment.merchant.dto.CaptureRequest;
import com.microservices.payment.merchant.dto.CaptureResponse;
import com.microservices.payment.merchant.dto.CreateOrderRequest;
import com.microservices.payment.merchant.dto.OrderResponse;
import com.microservices.payment.merchant.dto.VerifyRequest;
import com.microservices.payment.merchant.entity.Merchant;
import com.microservices.payment.merchant.entity.MerchantOrder;
import com.microservices.payment.merchant.entity.OrderPayment;
import com.microservices.payment.merchant.provider.PaymentProvider;
import com.microservices.payment.merchant.repository.MerchantOrderRepository;
import com.microservices.payment.merchant.repository.OrderPaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
public class OrderService {

    private final MerchantOrderRepository orderRepository;
    private final OrderPaymentRepository paymentRepository;
    private final PaymentProvider paymentProvider;
    private final CryptoService crypto;

    public OrderService(MerchantOrderRepository orderRepository,
                        OrderPaymentRepository paymentRepository,
                        PaymentProvider paymentProvider,
                        CryptoService crypto) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.paymentProvider = paymentProvider;
        this.crypto = crypto;
    }

    public Mono<OrderResponse> createOrder(Merchant merchant, CreateOrderRequest request) {
        String currency = (request.getCurrency() == null || request.getCurrency().isBlank())
                ? "INR" : request.getCurrency().toUpperCase();
        Instant now = Instant.now();

        MerchantOrder order = MerchantOrder.builder()
                .orderRef("order_" + CryptoService.randomToken(18))
                .merchantId(merchant.getId())
                .amount(request.getAmount())
                .currency(currency)
                .receipt(request.getReceipt())
                .notes(request.getNotes())
                .status(OrderStatus.CREATED)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return orderRepository.save(order)
                .doOnSuccess(o -> log.info("Order {} created for merchant {}",
                        o.getOrderRef(), merchant.getId()))
                .map(OrderResponse::from);
    }

    public Mono<OrderResponse> getOrder(Merchant merchant, String orderRef) {
        return findScopedOrder(merchant, orderRef).map(OrderResponse::from);
    }

    public Flux<OrderResponse> listOrders(Merchant merchant) {
        return orderRepository.findByMerchantIdOrderByIdDesc(merchant.getId())
                .map(OrderResponse::from);
    }

    /** Mock-PSP capture: charges via the provider, records the payment and signs the result. */
    public Mono<CaptureResponse> capture(Merchant merchant, String orderRef, CaptureRequest req) {
        return findScopedOrder(merchant, orderRef)
                .flatMap(order -> {
                    if (order.getStatus() == OrderStatus.PAID) {
                        return Mono.error(new BusinessException("Order is already paid"));
                    }
                    order.setStatus(OrderStatus.ATTEMPTED);
                    order.setUpdatedAt(Instant.now());

                    var charge = new PaymentProvider.ProviderCharge(
                            order.getOrderRef(), order.getAmount(), order.getCurrency(),
                            req.getMethod(), req.getVpa(), req.isSimulateFailure());

                    return orderRepository.save(order)
                            .then(paymentProvider.charge(charge))
                            .flatMap(result -> recordResult(merchant, order, req, result));
                });
    }

    private Mono<CaptureResponse> recordResult(Merchant merchant, MerchantOrder order,
                                               CaptureRequest req,
                                               PaymentProvider.ProviderResult result) {
        String paymentRef = "pay_" + CryptoService.randomToken(18);
        OrderPayment payment = OrderPayment.builder()
                .paymentRef(paymentRef)
                .orderRef(order.getOrderRef())
                .merchantId(merchant.getId())
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .method(req.getMethod())
                .status(result.success() ? "CAPTURED" : "FAILED")
                .createdAt(Instant.now())
                .build();

        order.setStatus(result.success() ? OrderStatus.PAID : OrderStatus.FAILED);
        order.setUpdatedAt(Instant.now());

        return paymentRepository.save(payment)
                .then(orderRepository.save(order))
                .map(saved -> {
                    if (!result.success()) {
                        return CaptureResponse.builder()
                                .paymentId(paymentRef)
                                .orderId(order.getOrderRef())
                                .status("failed")
                                .amount(order.getAmount())
                                .currency(order.getCurrency())
                                .build();
                    }
                    String signature = sign(order.getOrderRef(), paymentRef, merchant);
                    return CaptureResponse.builder()
                            .paymentId(paymentRef)
                            .orderId(order.getOrderRef())
                            .signature(signature)
                            .status("captured")
                            .amount(order.getAmount())
                            .currency(order.getCurrency())
                            .build();
                });
    }

    /** Verifies the signature a merchant received, scoped to that merchant's secret. */
    public Mono<Boolean> verify(Merchant merchant, VerifyRequest req) {
        return Mono.fromCallable(() -> {
            String expected = sign(req.getOrderId(), req.getPaymentId(), merchant);
            return crypto.constantTimeEquals(expected, req.getSignature());
        });
    }

    private String sign(String orderRef, String paymentRef, Merchant merchant) {
        String secret = crypto.decrypt(merchant.getKeySecretEnc());
        return crypto.hmacSha256Hex(orderRef + "|" + paymentRef, secret);
    }

    private Mono<MerchantOrder> findScopedOrder(Merchant merchant, String orderRef) {
        return orderRepository.findByOrderRef(orderRef)
                .filter(o -> o.getMerchantId().equals(merchant.getId()))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Order", orderRef)));
    }
}
