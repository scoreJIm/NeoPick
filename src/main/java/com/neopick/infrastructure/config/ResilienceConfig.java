package com.neopick.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    private static final int FAILURE_THRESHOLD = 3;
    private static final Duration OPEN_STATE_DURATION = Duration.ofSeconds(30);
    private static final Duration TIMEOUT_DURATION = Duration.ofSeconds(15);

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(3)
                .failureRateThreshold(50)
                .waitDurationInOpenState(OPEN_STATE_DURATION)
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();

        return CircuitBreakerRegistry.of(defaultConfig);
    }

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig defaultConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(2))
                .build();

        return RetryRegistry.of(defaultConfig);
    }

    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig defaultConfig = TimeLimiterConfig.custom()
                .timeoutDuration(TIMEOUT_DURATION)
                .build();

        return TimeLimiterRegistry.of(defaultConfig);
    }

    @Bean
    public CircuitBreaker smsServiceCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("smsService");
    }

    @Bean
    public CircuitBreaker paymentGatewayCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("paymentGateway");
    }

    @Bean
    public CircuitBreaker s3StorageCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("s3Storage");
    }

    @Bean
    public Retry s3StorageRetry(RetryRegistry registry) {
        return registry.retry("s3Storage");
    }

    @Bean
    public TimeLimiter paymentGatewayTimeLimiter(TimeLimiterRegistry registry) {
        return registry.timeLimiter("paymentGateway");
    }
}
