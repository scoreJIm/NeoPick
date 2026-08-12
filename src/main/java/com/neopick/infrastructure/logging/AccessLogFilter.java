package com.neopick.infrastructure.logging;

import com.neopick.adapter.web.security.SecurityContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class AccessLogFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AccessLogFilter.class);

    private final SecurityContextHolder securityContextHolder;

    public AccessLogFilter(SecurityContextHolder securityContextHolder) {
        this.securityContextHolder = securityContextHolder;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            // Ensure userId is in MDC from security context (fallback if not set by auth filter)
            securityContextHolder.getCurrentUserId().ifPresent(uid -> MDC.put("userId", uid));
            log.info("{} {} - {}ms", httpRequest.getMethod(),
                    httpRequest.getRequestURI(), duration);
            MDC.clear();
        }
    }
}
