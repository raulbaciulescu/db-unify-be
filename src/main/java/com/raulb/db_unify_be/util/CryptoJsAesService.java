package com.raulb.db_unify_be.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

@Component
public class CryptoJsAesService {

    @Value("${dbunify.encryption.secret}")
    private String SECRET;

    public String decrypt(String encryptedBase64) {
        try {
            byte[] ciphertextBytes = Base64.getDecoder().decode(encryptedBase64);

            // Verific prefix "Salted__"
            byte[] saltedPrefix = Arrays.copyOfRange(ciphertextBytes, 0, 8);
            if (!"Salted__".equals(new String(saltedPrefix, StandardCharsets.UTF_8))) {
                throw new IllegalArgumentException("Encrypted data missing 'Salted__' prefix.");
            }

            byte[] salt = Arrays.copyOfRange(ciphertextBytes, 8, 16);
            byte[] encrypted = Arrays.copyOfRange(ciphertextBytes, 16, ciphertextBytes.length);

            byte[] keyAndIV = EVP_BytesToKey(SECRET.getBytes(StandardCharsets.UTF_8), salt, 32, 16);
            byte[] key = Arrays.copyOfRange(keyAndIV, 0, 32);
            byte[] iv = Arrays.copyOfRange(keyAndIV, 32, 48);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt password", e);
        }
    }

    private byte[] EVP_BytesToKey(byte[] password, byte[] salt, int keyLen, int ivLen) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] keyAndIv = new byte[keyLen + ivLen];
        byte[] prev = new byte[0];
        int i = 0;
        while (i < keyAndIv.length) {
            md.reset();
            md.update(prev);
            md.update(password);
            md.update(salt);
            byte[] digest = md.digest();
            int toCopy = Math.min(digest.length, keyAndIv.length - i);
            System.arraycopy(digest, 0, keyAndIv, i, toCopy);
            i += toCopy;
            prev = digest;
        }
        return keyAndIv;
    }
}
