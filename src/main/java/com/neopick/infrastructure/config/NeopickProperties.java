package com.neopick.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "neopick")
public record NeopickProperties(
        JwtProperties jwt,
        SmsProperties sms,
        AwsProperties aws,
        BookingProperties booking,
        S3Properties s3,
        CdnProperties cdn,
        MediaProperties media,
        PaymentProperties payment
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

    public record S3Properties(
            String bucket,
            String region,
            int presignUploadDuration,
            int presignDownloadDuration
    ) {}

    public record CdnProperties(
            String domain
    ) {}

    public record MediaProperties(
            long maxFileSize,
            List<String> allowedTypes
    ) {}

    public record PaymentProperties(
            AlipayProperties alipay,
            WechatProperties wechat
    ) {
        public record AlipayProperties(
                String appId,
                String privateKey,
                String alipayPublicKey,
                String gatewayUrl,
                String notifyUrl,
                String returnUrl
        ) {}

        public record WechatProperties(
                String appId,
                String mchId,
                String apiKey,
                String notifyUrl
        ) {}
    }
}
