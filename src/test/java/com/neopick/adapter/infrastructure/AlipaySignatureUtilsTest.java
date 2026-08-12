package com.neopick.adapter.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("AlipaySignatureUtils")
class AlipaySignatureUtilsTest {

    private static final String TEST_PRIVATE_KEY;
    private static final String TEST_PUBLIC_KEY;

    static {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();
            TEST_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n"
                    + Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded())
                    + "\n-----END PRIVATE KEY-----";
            TEST_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n"
                    + Base64.getEncoder().encodeToString(pair.getPublic().getEncoded())
                    + "\n-----END PUBLIC KEY-----";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("Sign and verify")
    class SignAndVerify {

        @Test
        @DisplayName("should sign params and verify successfully")
        void shouldSignAndVerifySuccessfully() {
            Map<String, String> params = buildSampleParams();

            String sign = AlipaySignatureUtils.sign(params, TEST_PRIVATE_KEY);

            assertThat(sign).isNotBlank();

            params.put("sign", sign);
            boolean valid = AlipaySignatureUtils.verify(params, sign, TEST_PUBLIC_KEY);

            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("should reject signature with wrong public key")
        void shouldRejectInvalidSignature() throws Exception {
            Map<String, String> params = buildSampleParams();

            String sign = AlipaySignatureUtils.sign(params, TEST_PRIVATE_KEY);

            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair otherPair = gen.generateKeyPair();
            String otherPublicKey = "-----BEGIN PUBLIC KEY-----\n"
                    + Base64.getEncoder().encodeToString(otherPair.getPublic().getEncoded())
                    + "\n-----END PUBLIC KEY-----";

            params.put("sign", sign);
            boolean valid = AlipaySignatureUtils.verify(params, sign, otherPublicKey);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("should reject tampered parameters")
        void shouldRejectTamperedParameters() {
            Map<String, String> params = buildSampleParams();

            String sign = AlipaySignatureUtils.sign(params, TEST_PRIVATE_KEY);

            Map<String, String> tampered = buildSampleParams();
            tampered.put("total_amount", "999.99");
            tampered.put("sign", sign);

            boolean valid = AlipaySignatureUtils.verify(tampered, sign, TEST_PUBLIC_KEY);

            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("Key loading")
    class KeyLoading {

        @Test
        @DisplayName("should handle PEM format keys")
        void shouldHandlePemFormatKeys() {
            Map<String, String> params = buildSampleParams();
            String sign = AlipaySignatureUtils.sign(params, TEST_PRIVATE_KEY);
            params.put("sign", sign);
            boolean valid = AlipaySignatureUtils.verify(params, sign, TEST_PUBLIC_KEY);
            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("should throw on invalid private key")
        void shouldThrowOnInvalidPrivateKey() {
            Map<String, String> params = buildSampleParams();
            assertThatThrownBy(() -> AlipaySignatureUtils.sign(params, "invalid-key"))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("Parameter ordering")
    class ParameterOrdering {

        @Test
        @DisplayName("should produce same signature regardless of insertion order")
        void shouldProduceSameSignatureRegardlessOfInsertionOrder() {
            Map<String, String> params1 = new LinkedHashMap<>();
            params1.put("app_id", "test");
            params1.put("method", "alipay.trade.page.pay");
            params1.put("total_amount", "100.00");

            Map<String, String> params2 = new LinkedHashMap<>();
            params2.put("total_amount", "100.00");
            params2.put("method", "alipay.trade.page.pay");
            params2.put("app_id", "test");

            String sign1 = AlipaySignatureUtils.sign(params1, TEST_PRIVATE_KEY);
            String sign2 = AlipaySignatureUtils.sign(params2, TEST_PRIVATE_KEY);

            assertThat(sign1).isEqualTo(sign2);
        }
    }

    private Map<String, String> buildSampleParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", "2021000000000001");
        params.put("method", "alipay.trade.page.pay");
        params.put("charset", "utf-8");
        params.put("sign_type", "RSA2");
        params.put("timestamp", "2024-01-01 12:00:00");
        params.put("version", "1.0");
        params.put("biz_content", "{\"out_trade_no\":\"test-001\",\"total_amount\":\"100.00\"}");
        return params;
    }
}
