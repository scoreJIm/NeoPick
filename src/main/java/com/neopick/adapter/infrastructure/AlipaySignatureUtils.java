package com.neopick.adapter.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class for Alipay RSA2 signature operations.
 */
final class AlipaySignatureUtils {

    private static final String SIGN_TYPE = "RSA2";
    private static final String SIGN_ALGORITHM = "SHA256withRSA";

    private AlipaySignatureUtils() {
    }

    /**
     * Generate a signature for the given parameters using the private key.
     * Parameters are sorted alphabetically and joined as key=value pairs separated by &amp;.
     */
    static String sign(Map<String, String> params, String privateKeyPem) {
        String content = buildSignContent(params);
        try {
            PrivateKey privateKey = loadPrivateKey(privateKeyPem);
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initSign(privateKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            byte[] signed = signature.sign();
            return Base64.getEncoder().encodeToString(signed);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign request", e);
        }
    }

    /**
     * Verify an Alipay callback signature using the Alipay public key.
     */
    static boolean verify(Map<String, String> params, String sign, String alipayPublicKeyPem) {
        String content = buildSignContent(params);
        try {
            PublicKey publicKey = loadPublicKey(alipayPublicKeyPem);
            Signature signature = Signature.getInstance(SIGN_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Build the string to be signed from parameters.
     * Keys are sorted alphabetically, excluding sign and sign_type.
     * Format: key1=value1&amp;key2=value2 (no trailing ampersand).
     */
    private static String buildSignContent(Map<String, String> params) {
        Map<String, String> sorted = new TreeMap<>(params);
        sorted.remove("sign");
        sorted.remove("sign_type");
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    static String signType() {
        return SIGN_TYPE;
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

    private static PublicKey loadPublicKey(String publicKeyPem) throws Exception {
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
