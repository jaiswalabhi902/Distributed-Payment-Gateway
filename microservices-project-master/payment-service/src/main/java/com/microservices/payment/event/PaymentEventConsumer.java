package com.microservices.payment.event;

import com.microservices.common.constant.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes payment events for audit/projection purposes. Kept side-effect free
 * here (logging); downstream services (settlement, AI) subscribe to the same topics.
 */
@Slf4j
@Component
public class PaymentEventConsumer {

    @KafkaListener(
            topics = {
                    Constants.Topics.PAYMENT_CREATED,
                    Constants.Topics.PAYMENT_UPDATED,
                    Constants.Topics.PAYMENT_REFUNDED
            },
            groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentEvent(PaymentEvent event) {
        log.info("Consumed event {} for payment {} (order {}), status={}",
                event.getEventType(), event.getPaymentId(),
                event.getOrderId(), event.getStatus());
    }
}
