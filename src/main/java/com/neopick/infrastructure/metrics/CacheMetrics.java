package com.neopick.infrastructure.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CacheMetrics {

    private static final Logger log = LoggerFactory.getLogger(CacheMetrics.class);

    public CacheMetrics(CacheManager cacheManager, MeterRegistry registry) {
        for (String cacheName : cacheManager.getCacheNames()) {
            List<Tag> tags = List.of(
                    Tag.of("cache", cacheName),
                    Tag.of("application", "neopick-api")
            );

            Gauge.builder("neopick.cache.registered", () -> 1.0)
                    .tags(tags)
                    .description("Cache registration status (1 = active)")
                    .register(registry);

            log.info("Cache metrics registered for: {}", cacheName);
        }
    }
}
