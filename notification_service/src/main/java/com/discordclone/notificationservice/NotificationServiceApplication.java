package com.discordclone.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {
        "com.discordclone.notificationservice",
        "com.discordclone.common"
})
@EnableDiscoveryClient
public class NotificationServiceApplication {

    public static void main(String[] args) {
        System.setProperty(
                "spring.config.additional-location",
                "optional:file:application.yml,optional:file:notification_service/application.yml"
        );
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
