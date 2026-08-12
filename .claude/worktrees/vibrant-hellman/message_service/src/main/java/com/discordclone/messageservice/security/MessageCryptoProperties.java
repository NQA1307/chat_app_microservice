package com.discordclone.messageservice.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.crypto.message")
public class MessageCryptoProperties {

    private String activeKeyId;

    /**
     * keyId -> Base64 encoded 32-byte AES key.
     */
    private Map<String, String> keys = new HashMap<>();
}