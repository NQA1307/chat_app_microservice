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
        SpringApplication.run(ServerServiceApplication.class, args);
    }
}
