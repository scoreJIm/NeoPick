package com.neopick.adapter.infrastructure;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for WeChat Pay V3 request signing, callback signature
 * verification and AES-256-GCM resource decryption.
 */
final class WechatSignatureUtils {

    static final String AUTH_SCHEMA = "WECHATPAY2-SHA256-RSA2048";
    static final String HEADER_SIGNATURE = "wechatpay-signature";
    static final String HEADER_TIMESTAMP = "wechatpay-timestamp";
    static final String HEADER_NONCE = "wechatpay-nonce";
    static final String HEADER_SERIAL = "wechatpay-serial";

    private static final String SIGN_ALGORITHM = "SHA256withRSA";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private WechatSignatureUtils() {
    }

    /**
     * Builds the {@code Authorization} header value for a WeChat Pay V3 API request.
     * The signed message is: method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n".
     */
    static String buildAuthorizationHeader(String method, String path, String body,
                                           String mchid, String serialNo, String privateKeyPem) {
        String nonce = generateNonce();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String message = buildRequestMessage(method, path, timestamp, nonce, body);
        String signature = sign(message, privateKeyPem);

        return AUTH_SCHEMA + " mchid=\"" + mchid + "\","
                + "nonce_str=\"" + nonce + "\","
                + "signature=\"" + signature + "\","
                + "timestamp=\"" + timestamp + "\","
                + "serial_no=\"" + serialNo + "\"";
    }

    /**
     * Verifies a WeChat Pay callback notification signature.
     * The signed message is: timestamp + "\n" + nonce + "\n" + body + "\n".
     */
    static boolean verifySignature(Map<String, String> headers, String body, String platformCertPem) {
        String timestamp = headers.get(HEADER_TIMESTAMP);
        String nonce = headers.get(HEADER_NONCE);
        String signature = headers.get(HEADER_SIGNATURE);
        if (timestamp == null || nonce == null || signature == null) {
            return false;
        }

        String message = buildNotificationMessage(timestamp, nonce, body);
        try {
            PublicKey publicKey = loadPublicKey(platformCertPem);
            Signature verifier = Signature.getInstance(SIGN_ALGORITHM);
            verifier.initVerify(publicKey);
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Decrypts the {@code resource} field of a WeChat Pay callback using AES-256-GCM.
     * The {@code ciphertext} is Base64-encoded and includes the GCM authentication tag.
     */
    static String decryptResource(String ciphertext, String nonce, String associatedData, String apiV3Key) {
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            SecretKeySpec keySpec = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS,
                    nonce.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
            if (associatedData != null && !associatedData.isEmpty()) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plaintext = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt WeChat Pay resource", e);
        }
    }

    static String sign(String message, String privateKeyPem) {
        try {
            PrivateKey privateKey = loadPrivateKey(privateKeyPem);
            Signature signer = Signature.getInstance(SIGN_ALGORITHM);
            signer.initSign(privateKey);
            signer.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign WeChat Pay message", e);
        }
    }

    private static String buildRequestMessage(String method, String path, String timestamp,
                                              String nonce, String body) {
        return method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
    }

    private static String buildNotificationMessage(String timestamp, String nonce, String body) {
        return timestamp + "\n" + nonce + "\n" + body + "\n";
    }

    private static String generateNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static PrivateKey loadPrivateKey(String privateKeyPem) throws Exception {
        String key = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }

    private static PublicKey loadPublicKey(String pem) throws Exception {
        if (pem.contains("BEGIN CERTIFICATE")) {
            return loadPublicKeyFromCertificate(pem);
        }
        return loadPublicKeyFromX509(pem);
    }

    private static PublicKey loadPublicKeyFromCertificate(String certPem) throws Exception {
        String cert = certPem
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
        byte[] certBytes = Base64.getDecoder().decode(cert);
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) factory
                .generateCertificate(new ByteArrayInputStream(certBytes));
        return certificate.getPublicKey();
    }

    private static PublicKey loadPublicKeyFromX509(String publicKeyPem) throws Exception {
        String key = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }
}
