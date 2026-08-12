package com.discordclone.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;

@Configuration
public class MeilisearchConfig {
    @Value("${meilisearch.host}")
    private String host;
    @Value("${meilisearch.master-key}")
    private String masterKey;

    @Bean
    public Client meilisearchClient() {
        return new Client(new Config(host, masterKey));
    }
}
