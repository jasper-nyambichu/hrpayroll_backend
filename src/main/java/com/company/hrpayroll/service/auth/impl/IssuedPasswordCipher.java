package com.company.hrpayroll.service.auth.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Encrypts the passwords the Admin sets for Branch Manager, Back Office and Agent logins (AES-256-GCM), so
 * the Admin can look one up after creating it. A copy of the database alone reveals none of them.
 *
 * Each one is bound to its user id, so it cannot be moved onto another account's row. The key is
 * CREDENTIAL_KEY, or else one derived from BIOMETRIC_TEMPLATE_KEY, so a server already set up needs nothing new.
 * Without either, a key derived from the JWT secret is used, which is only fit for a developer's PC.
 */
@Slf4j
@Component
public class IssuedPasswordCipher {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;

    public IssuedPasswordCipher(
            @Value("${app.credentials.key:}") String credentialKey,
            @Value("${app.biometric.template-key:}") String templateKey,
            @Value("${app.jwt.secret}") String jwtSecret
    ) {
        byte[] own = decode(credentialKey);
        if (own != null && own.length == 32) {
            this.key = new SecretKeySpec(own, "AES");
            return;
        }
        byte[] base = decode(templateKey);
        if (base == null || base.length != 32) {
            log.warn("Neither CREDENTIAL_KEY nor BIOMETRIC_TEMPLATE_KEY is set: saved logins are encrypted with a key "
                    + "derived from the JWT secret. Set CREDENTIAL_KEY on a live server.");
            base = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        this.key = new SecretKeySpec(derive(base), "AES");
    }

    public byte[] encrypt(String password, Long userId) {
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        try {
            byte[] sealed = cipher(Cipher.ENCRYPT_MODE, iv, userId).doFinal(password.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(IV_BYTES + sealed.length).put(iv).put(sealed).array();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Could not encrypt the login's password", ex);
        }
    }

    // Null when it cannot be read, for example after the key was changed.
    public String decrypt(byte[] stored, Long userId) {
        try {
            byte[] iv = Arrays.copyOfRange(stored, 0, IV_BYTES);
            byte[] plain = cipher(Cipher.DECRYPT_MODE, iv, userId).doFinal(stored, IV_BYTES, stored.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | RuntimeException ex) {
            log.warn("Could not decrypt the saved password of user {}: {}", userId, ex.getMessage());
            return null;
        }
    }

    private Cipher cipher(int mode, byte[] iv, Long userId) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, iv));
        cipher.updateAAD(("user-password:" + userId).getBytes(StandardCharsets.UTF_8));
        return cipher;
    }

    // A separate key for this purpose, so it never matches the fingerprint key itself.
    private static byte[] derive(byte[] base) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(base, "HmacSHA256"));
            return mac.doFinal("dhwarsh-issued-password-v1".getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static byte[] decode(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
