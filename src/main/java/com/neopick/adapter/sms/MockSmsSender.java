package com.neopick.adapter.sms;

import com.neopick.port.sms.SmsSender;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile({"dev", "test", "local"})
public class MockSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(MockSmsSender.class);
    private final Map<String, String> codeStore = new ConcurrentHashMap<>();

    @Override
    @CircuitBreaker(name = "smsService", fallbackMethod = "sendFallback")
    public void sendVerificationCode(String phone, String code) {
        codeStore.put(phone, code);
        log.info("[MOCK SMS] Sending code {} to phone {}", code, phone);
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        String stored = codeStore.get(phone);
        boolean valid = stored != null && stored.equals(code);
        if (valid) {
            codeStore.remove(phone);
        }
        log.info("[MOCK SMS] Code verification for {} : {}", phone, valid ? "PASS" : "FAIL");
        return valid;
    }

    @SuppressWarnings("unused")
    void sendFallback(String phone, String code, Throwable t) {
        log.error("[MOCK SMS] Circuit breaker open — SMS not sent to {}", phone, t);
    }
}
