package com.discordclone.messageservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {
        "com.discordclone.messageservice",
        "com.discordclone.common"
})
@EnableDiscoveryClient
public class MessageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessageServiceApplication.class, args);
    }
}
