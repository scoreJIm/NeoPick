package com.neopick.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for the WeChat Pay V3 gateway integration.
 * Exposes a RestTemplate bean configured for WeChat Pay API communication.
 */
@Configuration
public class WechatConfig {

    @Bean
    public RestTemplate wechatRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }
}
