package com.neopick.adapter.sms;

import com.neopick.domain.auth.SmsCodeService;
import com.neopick.port.cache.CacheManager;
import com.neopick.port.sms.SmsSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class SmsCodeServiceImpl implements SmsCodeService {

    private static final int CODE_LENGTH = 6;
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration RATE_LIMIT = Duration.ofSeconds(60);
    private static final String CODE_PREFIX = "sms:code:";
    private static final String RATE_PREFIX = "sms:rate:";

    private final SmsSender smsSender;
    private final CacheManager cacheManager;

    public SmsCodeServiceImpl(SmsSender smsSender, CacheManager cacheManager) {
        this.smsSender = smsSender;
        this.cacheManager = cacheManager;
    }

    @Override
    public void sendCode(String phone) {
        String rateKey = RATE_PREFIX + phone;
        if (cacheManager.exists(rateKey)) {
            throw new IllegalStateException("Please wait before requesting another code");
        }
        String code = generateCode();
        String codeKey = CODE_PREFIX + phone;
        cacheManager.set(codeKey, code, CODE_TTL);
        cacheManager.set(rateKey, "1", RATE_LIMIT);
        smsSender.sendVerificationCode(phone, code);
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        String codeKey = CODE_PREFIX + phone;
        return cacheManager.get(codeKey)
                .map(stored -> stored.equals(code))
                .orElse(false);
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
