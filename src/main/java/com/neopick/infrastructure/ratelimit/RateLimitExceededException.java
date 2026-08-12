package com.neopick.infrastructure.ratelimit;

/**
 * Thrown by {@link RateLimitAspect} when a rate limit is exceeded.
 * Carries the retry-after seconds for the Retry-After HTTP header.
 */
public class RateLimitExceededException extends RuntimeException {

    private final String endpoint;
    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, String endpoint, long retryAfterSeconds) {
        super(message);
        this.endpoint = endpoint;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
