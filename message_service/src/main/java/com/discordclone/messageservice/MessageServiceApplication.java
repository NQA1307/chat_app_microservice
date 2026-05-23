package com.discordclone.messageservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication(scanBasePackages = {
        "com.discordclone.messageservice",
        "com.discordclone.common"
})
@EnableDiscoveryClient
public class MessageServiceApplication {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    public static void main(String[] args) {
        System.setProperty(
                "spring.config.additional-location",
                "optional:file:application.yml,optional:file:message_service/application.yml"
        );
        SpringApplication.run(MessageServiceApplication.class, args);
    }
}
