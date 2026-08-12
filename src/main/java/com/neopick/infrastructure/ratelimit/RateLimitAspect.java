package com.neopick.infrastructure.ratelimit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AOP aspect that enforces {@link RateLimit} annotations on controller methods.
 *
 * Uses a Redis ZSET-based sliding window algorithm. Each request adds the
 * current timestamp as a scored member to a sorted set, expired entries are
 * pruned, and the remaining count is checked against the limit.
 *
 * Falls back to an in-memory ConcurrentHashMap sliding window when Redis is
 * unreachable.
 */
@Aspect
@Component
@ConditionalOnBean(StringRedisTemplate.class)
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    /** In-memory fallback when Redis is unavailable. */
    private final ConcurrentMap<String, FallbackWindow> fallbackMap = new ConcurrentHashMap<>();

    /** Reusable counter instances, keyed by endpoint tag value. */
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();

    public RateLimitAspect(StringRedisTemplate redisTemplate, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(com.neopick.infrastructure.ratelimit.RateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit annotation = method.getAnnotation(RateLimit.class);

        int limit = annotation.limit();
        int windowSeconds = annotation.windowSeconds();
        String scope = annotation.scope();
        String identifier = resolveIdentifier(scope);
        String endpoint = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        String redisKey = "ratelimit:" + scope.toLowerCase() + ":" + identifier + ":" + endpoint;

        try {
            checkRedisLimit(redisKey, limit, windowSeconds, endpoint);
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis unavailable for rate limiting, falling back to in-memory: {}", e.getMessage());
            checkFallbackLimit(redisKey, limit, windowSeconds, endpoint);
        }

        setRateLimitHeaders(limit, redisKey, windowSeconds);

        return joinPoint.proceed();
    }

    // ---- Redis ZSET sliding window ----

    private void checkRedisLimit(String key, int limit, int windowSeconds, String endpoint) {
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        // Remove entries outside the current window
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // Count entries in the window (before adding the current request)
        Long count = redisTemplate.opsForZSet().zCard(key);
        long currentCount = count != null ? count : 0;

        if (currentCount >= limit) {
            recordHit(endpoint);
            long retryAfter = computeRetryAfter(key, windowSeconds);
            log.warn("Rate limit exceeded: key={} count={} limit={}", key, currentCount, limit);
            throw new RateLimitExceededException(
                    "Too many requests. Retry after " + retryAfter + " seconds",
                    endpoint, retryAfter);
        }

        // Add the current request with a unique member
        String member = now + ":" + ThreadLocalRandom.current().nextLong();
        redisTemplate.opsForZSet().add(key, member, now);

        // Refresh TTL so idle keys expire
        redisTemplate.expire(key, Duration.ofSeconds(windowSeconds + 1));
    }

    private long computeRetryAfter(String key, int windowSeconds) {
        try {
            // Find the oldest entry in the window to estimate reset time
            var oldestSet = redisTemplate.opsForZSet().rangeWithScores(key, 0, 0);
            if (oldestSet != null && !oldestSet.isEmpty()) {
                double oldestScore = oldestSet.iterator().next().getScore();
                long resetAt = (long) oldestScore + (windowSeconds * 1000L);
                long retryAfter = Math.max(1, (resetAt - System.currentTimeMillis()) / 1000);
                return retryAfter > windowSeconds ? windowSeconds : retryAfter;
            }
        } catch (Exception ignored) {
        }
        return windowSeconds;
    }

    private void setRateLimitHeaders(int limit, String key, int windowSeconds) {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletResponse response = attrs.getResponse();
                if (response != null) {
                    Long count = redisTemplate.opsForZSet().zCard(key);
                    long remaining = Math.max(0, limit - (count != null ? count : 0));
                    response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
                    response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
                    response.setHeader("X-RateLimit-Reset",
                            String.valueOf(System.currentTimeMillis() / 1000 + windowSeconds));
                }
            }
        } catch (Exception ignored) {
        }
    }

    // ---- In-memory fallback ----

    private void checkFallbackLimit(String key, int limit, int windowSeconds, String endpoint) {
        long now = System.currentTimeMillis();
        long windowMillis = windowSeconds * 1000L;

        FallbackWindow window = fallbackMap.compute(key, (k, v) -> {
            if (v == null || now - v.windowStart > windowMillis) {
                return new FallbackWindow(now, 1);
            }
            v.count.incrementAndGet();
            return v;
        });

        if (window.count.get() > limit) {
            recordHit(endpoint);
            long elapsed = now - window.windowStart;
            long retryAfter = Math.max(1, windowSeconds - elapsed / 1000);
            log.warn("Rate limit exceeded (fallback): key={} count={} limit={}", key, window.count.get(), limit);
            throw new RateLimitExceededException(
                    "Too many requests. Retry after " + retryAfter + " seconds",
                    endpoint, retryAfter);
        }
    }

    // ---- Identifier resolution ----

    private String resolveIdentifier(String scope) {
        if ("USER".equalsIgnoreCase(scope)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return auth.getName();
            }
        }
        return getClientIp();
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xff = request.getHeader("X-Forwarded-For");
                if (xff != null && !xff.isBlank()) {
                    return xff.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    // ---- Metrics ----

    private void recordHit(String endpoint) {
        Counter counter = counters.computeIfAbsent(endpoint, ep ->
                Counter.builder("neopick.ratelimit.hits")
                        .tag("endpoint", ep)
                        .description("Number of rate limit exceeded events")
                        .register(meterRegistry));
        counter.increment();
    }

    // ---- Inner types ----

    private static class FallbackWindow {
        final long windowStart;
        final AtomicLong count;

        FallbackWindow(long windowStart, long initialCount) {
            this.windowStart = windowStart;
            this.count = new AtomicLong(initialCount);
        }
    }
}
