package com.discordclone.userservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {
        "com.discordclone.userservice",
        "com.discordclone.common"          // scan GlobalExceptionHandler from common
})
@EnableDiscoveryClient
public class UserServiceApplication {
    public static void main(String[] args) {
        System.setProperty(
                "spring.config.additional-location",
                "optional:file:application.yml,optional:file:user_service/application.yml"
        );
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
