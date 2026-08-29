package com.neopick.adapter.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.teaopenapi.models.Config;
import com.neopick.infrastructure.config.NeopickProperties;
import com.neopick.port.sms.SmsSender;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Production SMS sender backed by Aliyun SMS (Dysmsapi).
 * <p>
 * Active only in the {@code prod} profile. Credentials are bound from
 * {@code neopick.sms.aliyun.*} (env vars in application-prod.yml).
 */
@Component
@Profile("prod")
public class AliyunSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(AliyunSmsSender.class);

    private final Client client;
    private final String signName;
    private final String templateCode;

    public AliyunSmsSender(NeopickProperties properties) {
        NeopickProperties.SmsProperties.AliyunProperties aliyun = properties.sms().aliyun();
        this.signName = aliyun.signName();
        this.templateCode = aliyun.templateCode();
        try {
            Config config = new Config();
            config.accessKeyId = aliyun.accessKeyId();
            config.accessKeySecret = aliyun.accessKeySecret();
            config.endpoint = "dysmsapi.aliyuncs.com";
            this.client = new Client(config);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize Aliyun SMS client", e);
        }
    }

    @Override
    @CircuitBreaker(name = "smsService", fallbackMethod = "sendFallback")
    public void sendVerificationCode(String phone, String code) {
        try {
            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam("{\"code\":\"" + code + "\"}");
            client.sendSms(request);
            log.info("[ALIYUN SMS] Verification code sent to {}", phone);
        } catch (Exception e) {
            log.error("[ALIYUN SMS] Failed to send code to {}: {}", phone, e.getMessage());
            throw new RuntimeException("SMS send failed", e);
        }
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        // Verification is handled by SmsCodeServiceImpl via Redis, not by the SMS provider.
        log.warn("[ALIYUN SMS] verifyCode is not the provider's responsibility");
        return false;
    }

    @SuppressWarnings("unused")
    void sendFallback(String phone, String code, Throwable t) {
        log.error("[ALIYUN SMS] Circuit breaker open — SMS not sent to {}", phone, t);
    }
}
