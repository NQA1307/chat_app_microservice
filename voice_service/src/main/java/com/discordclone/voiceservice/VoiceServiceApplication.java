package com.discordclone.voiceservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication(scanBasePackages = {
        "com.discordclone.voiceservice",
        "com.discordclone.common"
})
@EnableDiscoveryClient
public class VoiceServiceApplication {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    public static void main(String[] args) {
        System.setProperty(
                "spring.config.additional-location",
                "optional:file:application.yml,optional:file:voice_service/application.yml"
        );
        SpringApplication.run(VoiceServiceApplication.class, args);
    }
}
