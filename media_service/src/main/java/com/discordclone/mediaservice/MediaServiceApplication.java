package com.discordclone.mediaservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {
        "com.discordclone.mediaservice",
        "com.discordclone.common"
})
@EnableDiscoveryClient
public class MediaServiceApplication {

    public static void main(String[] args) {
        System.setProperty(
                "spring.config.additional-location",
                "optional:file:application.yml,optional:file:media_service/application.yml"
        );
        SpringApplication.run(MediaServiceApplication.class, args);
    }
}
