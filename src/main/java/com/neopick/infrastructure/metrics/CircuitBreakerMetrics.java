package com.neopick.infrastructure.metrics;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CircuitBreakerMetrics {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerMetrics.class);
    private static final String GAUGE_NAME = "neopick.circuitbreaker.state";

    private final MeterRegistry meterRegistry;
    private final Map<String, CircuitBreakerStateHolder> stateHolders = new ConcurrentHashMap<>();

    public CircuitBreakerMetrics(MeterRegistry meterRegistry,
                                  CircuitBreakerRegistry circuitBreakerRegistry) {
        this.meterRegistry = meterRegistry;
        circuitBreakerRegistry.getAllCircuitBreakers()
                .forEach(this::registerGauge);
    }

    @EventListener
    public void onStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        String name = event.getCircuitBreakerName();
        CircuitBreaker.State newState = event.getStateTransition().getToState();
        int stateValue = stateToValue(newState);

        CircuitBreakerStateHolder holder = stateHolders.get(name);
        if (holder != null) {
            holder.setStateValue(stateValue);
        }
        log.info("Circuit breaker [{}] state changed to {} (value={})", name, newState, stateValue);
    }

    private void registerGauge(CircuitBreaker circuitBreaker) {
        String name = circuitBreaker.getName();
        int initialState = stateToValue(circuitBreaker.getState());
        CircuitBreakerStateHolder holder = new CircuitBreakerStateHolder(initialState);

        Gauge.builder(GAUGE_NAME, holder, CircuitBreakerStateHolder::getStateValue)
                .description("Current state of circuit breaker: 0=CLOSED, 1=OPEN, 2=HALF_OPEN")
                .tags(List.of(Tag.of("name", name)))
                .register(meterRegistry);

        stateHolders.put(name, holder);
        log.info("Registered circuit breaker gauge for [{}] with initial state {}", name, initialState);
    }

    private static int stateToValue(CircuitBreaker.State state) {
        return switch (state) {
            case CLOSED -> 0;
            case OPEN -> 1;
            case HALF_OPEN -> 2;
            case FORCED_OPEN -> 1;
            case DISABLED -> 0;
            case METRICS_ONLY -> 0;
            default -> 0;
        };
    }

    static class CircuitBreakerStateHolder {
        private volatile int stateValue;

        CircuitBreakerStateHolder(int initialState) {
            this.stateValue = initialState;
        }

        int getStateValue() {
            return stateValue;
        }

        void setStateValue(int value) {
            this.stateValue = value;
        }
    }
}
