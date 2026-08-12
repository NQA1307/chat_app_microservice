package com.discordclone.serverservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {
        "com.discordclone.serverservice",
        "com.discordclone.common"  // để GlobalExceptionHandler từ common được load
})
@EnableDiscoveryClient
public class ServerServiceApplication {

    public static void main(String[] args) {
        System.setProperty(
                "spring.config.additional-location",
                "optional:file:application.yml,optional:file:server_service/application.yml"
        );
        SpringApplication.run(ServerServiceApplication.class, args);
    }
}
