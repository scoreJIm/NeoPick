package com.neopick.infrastructure.ratelimit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<String, SlidingWindow> windows = new ConcurrentHashMap<>();
    private final Counter rateLimitHits;

    private static final int SMS_MAX_REQUESTS = 5;
    private static final long SMS_WINDOW_SECONDS = 60;
    private static final int LOGIN_MAX_REQUESTS = 10;
    private static final long LOGIN_WINDOW_SECONDS = 60;

    public RateLimitInterceptor(MeterRegistry registry) {
        this.rateLimitHits = Counter.builder("neopick.ratelimit.hits")
                .description("Number of rate limit hits")
                .register(registry);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        if (path.equals("/api/v1/auth/send-sms-code")) {
            return checkLimit(clientIp + ":sms", SMS_MAX_REQUESTS, SMS_WINDOW_SECONDS, response);
        }
        if (path.equals("/api/v1/auth/login")) {
            return checkLimit(clientIp + ":login", LOGIN_MAX_REQUESTS, LOGIN_WINDOW_SECONDS, response);
        }
        return true;
    }

    private boolean checkLimit(String key, int maxRequests, long windowSeconds,
                               HttpServletResponse response) throws Exception {
        SlidingWindow window = windows.computeIfAbsent(key,
                k -> new SlidingWindow(maxRequests, windowSeconds));
        if (!window.tryAcquire()) {
            rateLimitHits.increment();
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many requests. Please try again later.\"}");
            return false;
        }
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    static class SlidingWindow {
        private final int maxRequests;
        private final long windowNanos;
        private long windowStart;
        private int counter;

        SlidingWindow(int maxRequests, long windowSeconds) {
            this.maxRequests = maxRequests;
            this.windowNanos = windowSeconds * 1_000_000_000L;
            this.windowStart = System.nanoTime();
        }

        synchronized boolean tryAcquire() {
            long now = System.nanoTime();
            if (now - windowStart > windowNanos) {
                windowStart = now;
                counter = 0;
            }
            if (counter >= maxRequests) {
                return false;
            }
            counter++;
            return true;
        }
    }
}
