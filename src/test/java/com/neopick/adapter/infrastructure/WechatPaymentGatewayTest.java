package com.neopick.adapter.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neopick.domain.booking.Booking;
import com.neopick.domain.booking.BookingId;
import com.neopick.domain.booking.BookingStatus;
import com.neopick.domain.payment.Payment;
import com.neopick.domain.payment.PaymentId;
import com.neopick.domain.payment.PaymentMethod;
import com.neopick.infrastructure.config.NeopickProperties;
import com.neopick.port.payment.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("WechatPaymentGateway")
class WechatPaymentGatewayTest {

    private static final String PRIVATE_KEY_PEM;
    private static final String PUBLIC_KEY_PEM;
    private static final String API_V3_KEY = "0123456789abcdef0123456789abcdef";

    static {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair();
            PRIVATE_KEY_PEM = "-----BEGIN PRIVATE KEY-----\n"
                    + Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded())
                    + "\n-----END PRIVATE KEY-----";
            PUBLIC_KEY_PEM = "-----BEGIN PUBLIC KEY-----\n"
                    + Base64.getEncoder().encodeToString(pair.getPublic().getEncoded())
                    + "\n-----END PUBLIC KEY-----";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private WechatPaymentGateway gateway;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        objectMapper = new ObjectMapper();
        NeopickProperties properties = new NeopickProperties(
                null, null, null, null, null, null, null,
                new NeopickProperties.PaymentProperties(
                        null,
                        new NeopickProperties.PaymentProperties.WechatProperties(
                                "wx-app-id", "wx-mch-id", API_V3_KEY, PRIVATE_KEY_PEM,
                                "serial-123", "https://api.neopick.com/notify/wechat", PUBLIC_KEY_PEM)));
        gateway = new WechatPaymentGateway(properties, restTemplate, objectMapper);
    }

    @Nested
    @DisplayName("Initiate payment")
    class InitiatePayment {

        @Test
        @DisplayName("should request a native QR code and return the code_url")
        void shouldInitiateNativePayment() {
            UUID paymentUuid = UUID.randomUUID();
            UUID bookingUuid = UUID.randomUUID();
            Payment payment = new Payment(new PaymentId(paymentUuid), bookingUuid.toString(),
                    new BigDecimal("300.00"), PaymentMethod.WECHAT);
            Booking booking = Booking.reconstruct(
                    new BookingId(bookingUuid), "student-1", 100L,
                    BookingStatus.PENDING_PAY, null, null, 0,
                    new BigDecimal("300.00"), null, null, null, null,
                    null, null, null, null, null, null);

            when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn("{\"code_url\":\"weixin://wxpay/bizpayurl?pr=abc123\"}");

            PaymentGateway.InitiatePaymentResult result = gateway.initiatePayment(payment, booking);

            assertThat(result.success()).isTrue();
            assertThat(result.tradeNo()).isEqualTo(paymentUuid.toString());
            assertThat(result.payUrl()).isEqualTo("weixin://wxpay/bizpayurl?pr=abc123");
            assertThat(result.qrCode()).isEqualTo("weixin://wxpay/bizpayurl?pr=abc123");
        }
    }

    @Nested
    @DisplayName("Callback verification")
    class CallbackVerification {

        @Test
        @DisplayName("should verify signature, decrypt resource and return a success result")
        void shouldVerifyAndDecryptSuccessCallback() throws Exception {
            String plaintext = "{\"out_trade_no\":\"order-001\",\"transaction_id\":\"wx-txn-001\","
                    + "\"trade_state\":\"SUCCESS\",\"amount\":{\"total\":30000,\"currency\":\"CNY\"}}";
            String nonce = "0123456789ab";
            String associatedData = "transaction";
            String ciphertext = encryptResource(plaintext, nonce, associatedData, API_V3_KEY);

            Map<String, String> params = buildCallbackParams(ciphertext, nonce, associatedData);

            PaymentGateway.CallbackResult result = gateway.verifyCallback(params);

            assertThat(result.signatureValid()).isTrue();
            assertThat(result.success()).isTrue();
            assertThat(result.tradeNo()).isEqualTo("wx-txn-001");
            assertThat(result.outTradeNo()).isEqualTo("order-001");
            assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
            assertThat(result.tradeStatus()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("should reject a callback with an invalid signature")
        void shouldRejectBadSignature() {
            String body = "{\"resource\":{\"ciphertext\":\"abcd\",\"nonce\":\"0123456789ab\","
                    + "\"associated_data\":\"transaction\"}}";

            Map<String, String> params = new HashMap<>();
            params.put(WechatPaymentGateway.CALLBACK_BODY_KEY, body);
            params.put(WechatPaymentGateway.CALLBACK_SIGNATURE_KEY, "invalid-signature");
            params.put(WechatPaymentGateway.CALLBACK_TIMESTAMP_KEY, "1700000000");
            params.put(WechatPaymentGateway.CALLBACK_NONCE_KEY, "abc123");
            params.put(WechatPaymentGateway.CALLBACK_SERIAL_KEY, "serial-123");

            PaymentGateway.CallbackResult result = gateway.verifyCallback(params);

            assertThat(result.signatureValid()).isFalse();
            assertThat(result.success()).isFalse();
        }
    }

    @Nested
    @DisplayName("Query payment")
    class QueryPayment {

        @Test
        @DisplayName("should parse trade state from the query response")
        void shouldQueryPayment() {
            String queryJson = "{\"out_trade_no\":\"order-001\",\"transaction_id\":\"wx-txn-001\","
                    + "\"trade_state\":\"SUCCESS\",\"amount\":{\"total\":30000,\"currency\":\"CNY\"}}";

            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(queryJson));

            PaymentGateway.QueryResult result = gateway.queryPayment("order-001");

            assertThat(result.success()).isTrue();
            assertThat(result.tradeNo()).isEqualTo("wx-txn-001");
            assertThat(result.outTradeNo()).isEqualTo("order-001");
            assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("300.00"));
            assertThat(result.tradeStatus()).isEqualTo("SUCCESS");
        }
    }

    private Map<String, String> buildCallbackParams(String ciphertext, String nonce, String associatedData)
            throws Exception {
        String body = "{\"resource\":{\"ciphertext\":\"" + ciphertext + "\","
                + "\"nonce\":\"" + nonce + "\",\"associated_data\":\"" + associatedData + "\"}}";
        String timestamp = "1700000000";
        String callbackNonce = "callback-nonce";
        String message = timestamp + "\n" + callbackNonce + "\n" + body + "\n";
        String signature = WechatSignatureUtils.sign(message, PRIVATE_KEY_PEM);

        Map<String, String> params = new HashMap<>();
        params.put(WechatPaymentGateway.CALLBACK_BODY_KEY, body);
        params.put(WechatPaymentGateway.CALLBACK_SIGNATURE_KEY, signature);
        params.put(WechatPaymentGateway.CALLBACK_TIMESTAMP_KEY, timestamp);
        params.put(WechatPaymentGateway.CALLBACK_NONCE_KEY, callbackNonce);
        params.put(WechatPaymentGateway.CALLBACK_SERIAL_KEY, "serial-123");
        return params;
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
}
