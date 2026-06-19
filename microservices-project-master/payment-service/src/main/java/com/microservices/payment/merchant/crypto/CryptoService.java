package com.microservices.payment.merchant.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Symmetric crypto for the merchant plane:
 * <ul>
 *   <li>AES-GCM encryption of each merchant's {@code key_secret} at rest (the raw
 *       secret is required to compute HMAC signatures, so it cannot be only hashed).</li>
 *   <li>HMAC-SHA256 signatures over order/payment ids, the same scheme merchants use
 *       to verify a payment server-side.</li>
 * </ul>
 */
@Service
public class CryptoService {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec aesKey;

    public CryptoService(@Value("${app.merchant.encryption-key}") String base64Key) {
        byte[] key = Base64.getDecoder().decode(base64Key);
        if (key.length != 32) {
            throw new IllegalStateException(
                    "app.merchant.encryption-key must be a base64-encoded 32-byte (AES-256) key");
        }
        this.aesKey = new SecretKeySpec(key, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] in = Base64.getDecoder().decode(encoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(in, 0, iv, 0, GCM_IV_LENGTH);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] pt = cipher.doFinal(in, GCM_IV_LENGTH, in.length - GCM_IV_LENGTH);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    /** {@code HMAC_SHA256(message, secret)} as lowercase hex — the merchant signature scheme. */
    public String hmacSha256Hex(String message, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(sig.length * 2);
            for (byte b : sig) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failed", e);
        }
    }

    public boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    /** Generates an opaque random token (base62-ish) of the given length. */
    public static String randomToken(int length) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
