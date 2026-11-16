package com.company.hrpayroll.service.attendance.impl;

import com.company.hrpayroll.exception.InvalidOperationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts fingerprint templates with AES-256-GCM, so a copy of the database alone reveals none.
 *
 * Each template is bound to its employee and finger, so one row's data cannot be pasted into another
 * employee's row to let them clock in as that person. Without a key this refuses rather than storing
 * anything readable.
 */
@Component
public class FingerprintCipher {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;

    public FingerprintCipher(@Value("${app.biometric.template-key:}") String base64Key) {
        this.key = parseKey(base64Key);
    }

    // A malformed key leaves it unconfigured, so the rest of the system still starts.
    private static SecretKeySpec parseKey(String base64Key) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64Key.trim());
            return bytes.length == KEY_BYTES ? new SecretKeySpec(bytes, "AES") : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public byte[] encrypt(byte[] template, Long employeeId, short fingerSlot) {
        return seal(template, employeeId + ":" + fingerSlot);
    }

    public byte[] decrypt(byte[] stored, Long employeeId, short fingerSlot) {
        return open(stored, employeeId + ":" + fingerSlot, "employee " + employeeId);
    }

    // A login's sign-in finger is bound to that login (and can never pass for an employee's, or another login's).
    public byte[] encryptForLogin(byte[] template, Long userId) {
        return seal(template, "login:" + userId);
    }

    public byte[] decryptForLogin(byte[] stored, Long userId) {
        return open(stored, "login:" + userId, "login " + userId);
    }

    private byte[] seal(byte[] template, String binding) {
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        try {
            byte[] sealed = cipher(Cipher.ENCRYPT_MODE, iv, binding).doFinal(template);
            return ByteBuffer.allocate(IV_BYTES + sealed.length).put(iv).put(sealed).array();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Could not encrypt the fingerprint template", ex);
        }
    }

    private byte[] open(byte[] stored, String binding, String owner) {
        try {
            byte[] iv = java.util.Arrays.copyOfRange(stored, 0, IV_BYTES);
            return cipher(Cipher.DECRYPT_MODE, iv, binding).doFinal(stored, IV_BYTES, stored.length - IV_BYTES);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Could not decrypt the fingerprint template of " + owner, ex);
        }
    }

    private Cipher cipher(int mode, byte[] iv, String binding) throws GeneralSecurityException {
        if (key == null) {
            throw new InvalidOperationException(
                    "Fingerprint storage is not set up: BIOMETRIC_TEMPLATE_KEY must be a base64 encoded 32 byte key");
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key, new GCMParameterSpec(TAG_BITS, iv));
        cipher.updateAAD(binding.getBytes(StandardCharsets.UTF_8));
        return cipher;
    }
}
