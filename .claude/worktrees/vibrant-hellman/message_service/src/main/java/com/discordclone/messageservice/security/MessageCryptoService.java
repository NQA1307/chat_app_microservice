package com.discordclone.messageservice.security;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessageCryptoService {

    private static final String PREFIX = "enc";
    private static final String VERSION = "v1";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int AES_256_KEY_LENGTH_BYTES = 32;

    private final MessageCryptoProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    private Map<String, SecretKeySpec> keyRing;
    private SecretKeySpec activeKey;


    //
    @PostConstruct
    void validateAndInitializeKeys() {
        if (!StringUtils.hasText(properties.getActiveKeyId())) {
            throw new IllegalStateException("app.crypto.message.active-key-id must be configured");
        }

        if (properties.getKeys() == null || properties.getKeys().isEmpty()) {
            throw new IllegalStateException("app.crypto.message.keys must contain at least one key");
        }

        Map<String, SecretKeySpec> loadedKeys = new HashMap<>();

        for (Map.Entry<String, String> entry : properties.getKeys().entrySet()) {
            String keyId = entry.getKey();
            String base64Key = entry.getValue();

            if (!StringUtils.hasText(keyId)) {
                throw new IllegalStateException("Message encryption key id cannot be blank");
            }

            if (!StringUtils.hasText(base64Key)) {
                throw new IllegalStateException("Message encryption key value cannot be blank for keyId=" + keyId);
            }

            byte[] keyBytes;
            try {
                keyBytes = Base64.getDecoder().decode(base64Key);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Message encryption key must be Base64 for keyId=" + keyId, e);
            }

            if (keyBytes.length != AES_256_KEY_LENGTH_BYTES) {
                throw new IllegalStateException(
                        "Message encryption key must decode to 32 bytes for keyId=" + keyId
                );
            }

            loadedKeys.put(keyId, new SecretKeySpec(keyBytes, "AES"));
        }

        SecretKeySpec configuredActiveKey = loadedKeys.get(properties.getActiveKeyId());
        if (configuredActiveKey == null) {
            throw new IllegalStateException(
                    "Active message encryption key not found: " + properties.getActiveKeyId()
            );
        }

        this.keyRing = Map.copyOf(loadedKeys);
        this.activeKey = configuredActiveKey;
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }

        if (isEncrypted(plaintext)) {
            return plaintext;
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    activeKey,
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            );

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            String payload = Base64.getEncoder().encodeToString(buffer.array());

            return PREFIX + ":" + VERSION + ":" + properties.getActiveKeyId() + ":" + payload;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt message content", e);
        }
    }

    public String decrypt(String value) {
        if (value == null) {
            return null;
        }

        if (!isEncrypted(value)) {
            return value;
        }

        EncryptedPayload encryptedPayload = parse(value);
        SecretKeySpec key = keyRing.get(encryptedPayload.keyId());

        if (key == null) {
            throw new IllegalStateException("Unknown message encryption key id: " + encryptedPayload.keyId());
        }

        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedPayload.payload());

            if (encryptedBytes.length <= IV_LENGTH_BYTES) {
                throw new IllegalStateException("Encrypted message payload is too short");
            }

            ByteBuffer buffer = ByteBuffer.wrap(encryptedBytes);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);

            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            );

            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt encrypted message content", e);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX + ":");
    }

    private EncryptedPayload parse(String value) {
        String[] parts = value.split(":", 4);

        if (parts.length != 4) {
            throw new IllegalStateException("Invalid encrypted message format");
        }

        if (!PREFIX.equals(parts[0])) {
            throw new IllegalStateException("Invalid encrypted message prefix");
        }

        if (!VERSION.equals(parts[1])) {
            throw new IllegalStateException("Unsupported encrypted message version: " + parts[1]);
        }

        if (!StringUtils.hasText(parts[2])) {
            throw new IllegalStateException("Encrypted message key id is blank");
        }

        if (!StringUtils.hasText(parts[3])) {
            throw new IllegalStateException("Encrypted message payload is blank");
        }

        return new EncryptedPayload(parts[2], parts[3]);
    }

    private record EncryptedPayload(String keyId, String payload) {
    }
}