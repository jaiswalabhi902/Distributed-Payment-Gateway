package com.microservices.payment.config;

import com.microservices.payment.domain.PaymentMethod;
import com.microservices.payment.domain.PaymentStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.MySqlDialect;

import java.util.List;

/**
 * Persists the payment enums as their {@code name()} string in VARCHAR columns,
 * independent of any driver-specific enum handling.
 */
@Configuration
public class R2dbcConfig {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        return R2dbcCustomConversions.of(MySqlDialect.INSTANCE, List.of(
                new PaymentStatusWriter(),
                new PaymentStatusReader(),
                new PaymentMethodWriter(),
                new PaymentMethodReader()));
    }

    @WritingConverter
    static class PaymentStatusWriter implements Converter<PaymentStatus, String> {
        @Override
        public String convert(PaymentStatus source) {
            return source.name();
        }
    }

    @ReadingConverter
    static class PaymentStatusReader implements Converter<String, PaymentStatus> {
        @Override
        public PaymentStatus convert(String source) {
            return PaymentStatus.valueOf(source);
        }
    }

    @WritingConverter
    static class PaymentMethodWriter implements Converter<PaymentMethod, String> {
        @Override
        public String convert(PaymentMethod source) {
            return source.name();
        }
    }

    @ReadingConverter
    static class PaymentMethodReader implements Converter<String, PaymentMethod> {
        @Override
        public PaymentMethod convert(String source) {
            return PaymentMethod.valueOf(source);
        }
    }
}
