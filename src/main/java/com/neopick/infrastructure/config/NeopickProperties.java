package com.neopick.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "neopick")
public record NeopickProperties(
        JwtProperties jwt,
        SmsProperties sms,
        AwsProperties aws,
        BookingProperties booking
) {

    public record JwtProperties(
            String secret,
            String accessTokenExpiration,
            String refreshTokenExpiration
    ) {}

    public record SmsProperties(
            String provider,
            int codeLength,
            int codeTtl,
            int resendCooldown,
            int dailyLimit,
            AliyunProperties aliyun
    ) {
        public record AliyunProperties(
                String accessKeyId,
                String accessKeySecret,
                String signName,
                String templateCode
        ) {}
    }

    public record AwsProperties(
            String region,
            S3Properties s3,
            SnsProperties sns
    ) {
        public record S3Properties(String bucket) {}
        public record SnsProperties(String bookingEventsTopic) {}
    }

    public record BookingProperties(
            String pendingConfirmTimeout,
            String pendingPayTimeout
    ) {}
}
