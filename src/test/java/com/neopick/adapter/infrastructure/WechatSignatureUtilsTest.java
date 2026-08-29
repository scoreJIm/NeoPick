package com.neopick.adapter.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;

@DisplayName("WechatSignatureUtils")
class WechatSignatureUtilsTest {

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
    @DisplayName("Authorization header")
    class AuthorizationHeader {

        @Test
        @DisplayName("should produce a verifiable WECHATPAY2-SHA256-RSA2048 signature")
        void shouldProduceVerifiableSignature() throws Exception {
            String method = "POST";
            String path = "/v3/pay/transactions/native";
            String body = "{\"out_trade_no\":\"order-001\"}";

            String header = WechatSignatureUtils.buildAuthorizationHeader(
                    method, path, body, "mch-123", "serial-456", TEST_PRIVATE_KEY);

            assertThat(header).startsWith("WECHATPAY2-SHA256-RSA2048 ");
            assertThat(header).contains("mchid=\"mch-123\"");
            assertThat(header).contains("serial_no=\"serial-456\"");

            String nonce = extractQuoted(header, "nonce_str");
            String timestamp = extractQuoted(header, "timestamp");
            String signature = extractQuoted(header, "signature");

            String message = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
            assertThat(verifyRsa(message, signature, TEST_PUBLIC_KEY)).isTrue();
        }

        @Test
        @DisplayName("should produce a different signature for a different body")
        void shouldProduceDifferentSignatureForDifferentBody() {
            String header1 = WechatSignatureUtils.buildAuthorizationHeader(
                    "POST", "/v3/pay/transactions/native", "{\"amount\":1}", "m", "s", TEST_PRIVATE_KEY);
            String header2 = WechatSignatureUtils.buildAuthorizationHeader(
                    "POST", "/v3/pay/transactions/native", "{\"amount\":2}", "m", "s", TEST_PRIVATE_KEY);

            assertThat(extractQuoted(header1, "signature"))
                    .isNotEqualTo(extractQuoted(header2, "signature"));
        }
    }

    @Nested
    @DisplayName("Callback signature verification")
    class CallbackVerification {

        @Test
        @DisplayName("should verify a valid callback signature")
        void shouldVerifyValidSignature() {
            String body = "{\"resource\":{}}";
            String timestamp = "1700000000";
            String nonce = "abc123";
            String message = timestamp + "\n" + nonce + "\n" + body + "\n";
            String signature = WechatSignatureUtils.sign(message, TEST_PRIVATE_KEY);

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put(WechatSignatureUtils.HEADER_TIMESTAMP, timestamp);
            headers.put(WechatSignatureUtils.HEADER_NONCE, nonce);
            headers.put(WechatSignatureUtils.HEADER_SIGNATURE, signature);

            assertThat(WechatSignatureUtils.verifySignature(headers, body, TEST_PUBLIC_KEY)).isTrue();
        }

        @Test
        @DisplayName("should reject a tampered callback body")
        void shouldRejectTamperedBody() {
            String body = "{\"resource\":{}}";
            String timestamp = "1700000000";
            String nonce = "abc123";
            String message = timestamp + "\n" + nonce + "\n" + body + "\n";
            String signature = WechatSignatureUtils.sign(message, TEST_PRIVATE_KEY);

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put(WechatSignatureUtils.HEADER_TIMESTAMP, timestamp);
            headers.put(WechatSignatureUtils.HEADER_NONCE, nonce);
            headers.put(WechatSignatureUtils.HEADER_SIGNATURE, signature);

            assertThat(WechatSignatureUtils.verifySignature(headers, "{\"resource\":\"tampered\"}", TEST_PUBLIC_KEY))
                    .isFalse();
        }

        @Test
        @DisplayName("should reject a signature from a different key")
        void shouldRejectWrongKey() throws Exception {
            String body = "{\"resource\":{}}";
            String timestamp = "1700000000";
            String nonce = "abc123";
            String message = timestamp + "\n" + nonce + "\n" + body + "\n";
            String signature = WechatSignatureUtils.sign(message, TEST_PRIVATE_KEY);

            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair other = gen.generateKeyPair();
            String otherPublicKey = "-----BEGIN PUBLIC KEY-----\n"
                    + Base64.getEncoder().encodeToString(other.getPublic().getEncoded())
                    + "\n-----END PUBLIC KEY-----";

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put(WechatSignatureUtils.HEADER_TIMESTAMP, timestamp);
            headers.put(WechatSignatureUtils.HEADER_NONCE, nonce);
            headers.put(WechatSignatureUtils.HEADER_SIGNATURE, signature);

            assertThat(WechatSignatureUtils.verifySignature(headers, body, otherPublicKey)).isFalse();
        }

        @Test
        @DisplayName("should reject missing signature headers")
        void shouldRejectMissingHeaders() {
            Map<String, String> headers = new LinkedHashMap<>();
            assertThat(WechatSignatureUtils.verifySignature(headers, "body", TEST_PUBLIC_KEY)).isFalse();
        }
    }

    @Nested
    @DisplayName("Resource decryption")
    class ResourceDecryption {

        private static final String API_V3_KEY = "0123456789abcdef0123456789abcdef";
        private static final String NONCE = "0123456789ab";

        @Test
        @DisplayName("should decrypt an AES-256-GCM encrypted resource")
        void shouldDecryptResource() throws Exception {
            String plaintext = "{\"out_trade_no\":\"order-001\",\"trade_state\":\"SUCCESS\"}";
            String associatedData = "transaction";
            String ciphertext = encryptResource(plaintext, NONCE, associatedData, API_V3_KEY);

            String decrypted = WechatSignatureUtils.decryptResource(
                    ciphertext, NONCE, associatedData, API_V3_KEY);

            assertThat(decrypted).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("should reject a tampered ciphertext")
        void shouldRejectTamperedCiphertext() throws Exception {
            String plaintext = "{\"out_trade_no\":\"order-001\"}";
            String associatedData = "transaction";
            String ciphertext = encryptResource(plaintext, NONCE, associatedData, API_V3_KEY);

            byte[] tampered = Base64.getDecoder().decode(ciphertext);
            tampered[0] ^= 0x01;
            String tamperedCiphertext = Base64.getEncoder().encodeToString(tampered);

            assertThatThrownBy(() -> WechatSignatureUtils.decryptResource(
                    tamperedCiphertext, NONCE, associatedData, API_V3_KEY))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    private static String encryptResource(String plaintext, String nonce, String associatedData, String key)
            throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
        if (associatedData != null && !associatedData.isEmpty()) {
            cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
        }
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private static boolean verifyRsa(String message, String signature, String publicKeyPem) throws Exception {
        String key = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.getDecoder().decode(key));
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(publicKey);
        verifier.update(message.getBytes(StandardCharsets.UTF_8));
        return verifier.verify(Base64.getDecoder().decode(signature));
    }

    private static String extractQuoted(String header, String key) {
        Matcher matcher = Pattern.compile(key + "=\"([^\"]*)\"").matcher(header);
        return matcher.find() ? matcher.group(1) : null;
    }
}
