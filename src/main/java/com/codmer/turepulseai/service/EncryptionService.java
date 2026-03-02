package com.codmer.turepulseai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class EncryptionService {

    @Value("${app.encryption-key:}")
    private String encryptionKeyString;

    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;

    /**
     * Encrypt plaintext using AES-256
     */
    public String encrypt(String plainText) throws Exception {
        SecretKey key = getSecretKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Decrypt ciphertext using AES-256
     */
    public String decrypt(String encryptedText) throws Exception {
        SecretKey key = getSecretKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * Get or create the secret key for encryption
     */
    private SecretKey getSecretKey() throws Exception {
        if (encryptionKeyString == null || encryptionKeyString.isEmpty()) {
            throw new IllegalStateException("Encryption key not configured. Set 'app.encryption-key' environment variable.");
        }

        // Decode the Base64-encoded key string
        byte[] decodedKey = Base64.getDecoder().decode(encryptionKeyString);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
    }

    /**
     * Generate a new encryption key (for initial setup)
     */
    public static String generateEncryptionKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_SIZE);
        SecretKey secretKey = keyGenerator.generateKey();
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }
}

