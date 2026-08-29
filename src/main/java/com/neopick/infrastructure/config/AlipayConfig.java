package com.neopick.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for the Alipay payment gateway integration.
 * Exposes a RestTemplate bean configured for Alipay sandbox communication.
 */
@Configuration
public class AlipayConfig {

    @Bean
    public RestTemplate alipayRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }
}
