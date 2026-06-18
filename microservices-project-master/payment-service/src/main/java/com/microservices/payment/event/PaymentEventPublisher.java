package com.microservices.payment.event;

import com.microservices.common.constant.Constants;
import com.microservices.payment.entity.Payment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Publishes payment lifecycle events to Kafka. Best-effort and non-blocking:
 * the actual send is offloaded to a bounded-elastic thread so it never blocks the
 * reactive event loop, and a Kafka outage is logged but never fails the
 * originating payment operation.
 */
@Slf4j
@Component
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishCreated(Payment payment) {
        publish(Constants.Topics.PAYMENT_CREATED, "PAYMENT_CREATED", payment);
    }

    public void publishUpdated(Payment payment) {
        publish(Constants.Topics.PAYMENT_UPDATED, "PAYMENT_UPDATED", payment);
    }

    public void publishRefunded(Payment payment) {
        publish(Constants.Topics.PAYMENT_REFUNDED, "PAYMENT_REFUNDED", payment);
    }

    private void publish(String topic, String eventType, Payment payment) {
        PaymentEvent event = PaymentEvent.builder()
                .eventType(eventType)
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .build();

        // Offload to a worker thread: kafkaTemplate.send() can block for up to
        // max.block.ms fetching metadata when the broker is unreachable, which must
        // never stall the reactive event loop. Fire-and-forget.
        Mono.fromRunnable(() -> doSend(topic, eventType, payment.getOrderId(), event))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private void doSend(String topic, String eventType, String key, PaymentEvent event) {
        try {
            kafkaTemplate.send(topic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} to {}: {}",
                                    eventType, topic, ex.getMessage());
                        } else {
                            log.debug("Published {} to {}", eventType, topic);
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing event {} to {}: {}", eventType, topic, e.getMessage());
        }
    }
}
