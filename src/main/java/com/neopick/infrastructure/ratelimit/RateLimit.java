package com.neopick.infrastructure.ratelimit;

import java.lang.annotation.*;

/**
 * Declarative rate limit annotation for controller methods.
 * Enforced by {@link RateLimitAspect} using Redis ZSET sliding window
 * with a ConcurrentHashMap fallback when Redis is unavailable.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** Maximum number of requests within the time window. */
    int limit() default 20;

    /** Time window in seconds. */
    int windowSeconds() default 60;

    /** Scope of rate limiting: IP or USER. */
    String scope() default "IP";
}
