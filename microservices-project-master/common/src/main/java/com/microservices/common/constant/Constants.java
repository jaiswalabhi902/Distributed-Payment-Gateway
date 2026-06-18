package com.microservices.common.constant;

/**
 * Cross-service constants: propagation headers, Kafka topics and role names.
 */
public final class Constants {

    private Constants() {
    }

    /** Headers the gateway sets after validating the JWT, relayed to downstream services. */
    public static final class Headers {
        public static final String USER_ID = "X-User-Id";
        public static final String USERNAME = "X-Username";
        public static final String ROLES = "X-User-Roles";

        private Headers() {
        }
    }

    /** Kafka topic names shared between producers and consumers. */
    public static final class Topics {
        public static final String PAYMENT_CREATED = "payment.created";
        public static final String PAYMENT_UPDATED = "payment.updated";
        public static final String PAYMENT_REFUNDED = "payment.refunded";

        private Topics() {
        }
    }

    /** Role names used for RBAC across services. */
    public static final class Roles {
        public static final String ADMIN = "ROLE_ADMIN";
        public static final String USER = "ROLE_USER";
        public static final String MERCHANT = "ROLE_MERCHANT";

        private Roles() {
        }
    }
}
