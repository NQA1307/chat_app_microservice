package com.discordclone.voiceservice;

import org.springframework.beans.factory.annotation.Value;
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
    public RestClient.Builder restClientBuilder(
            @Value("${internal.service-secret}") String internalServiceSecret
    ) {
        return RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().set("X-Internal-Secret", internalServiceSecret);
                    return execution.execute(request, body);
                });
    }

    public static void main(String[] args) {
        System.setProperty(
                "spring.config.additional-location",
                "optional:file:application.yml,optional:file:voice_service/application.yml"
        );
        SpringApplication.run(VoiceServiceApplication.class, args);
    }
}
