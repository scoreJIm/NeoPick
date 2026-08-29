package com.neopick.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    private final NeopickProperties properties;

    public S3Config(NeopickProperties properties) {
        this.properties = properties;
    }

    @Bean
    public S3Client s3Client() {
        Region region = Region.of(properties.aws().region());
        return S3Client.builder()
                .region(region)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        Region region = Region.of(properties.aws().region());
        return S3Presigner.builder()
                .region(region)
                .build();
    }
}
