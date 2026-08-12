package com.discordclone.mediaservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication(scanBasePackages = {
        "com.discordclone.mediaservice",
        "com.discordclone.common"
})
@EnableDiscoveryClient
public class MediaServiceApplication {

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
                "optional:file:application.yml,optional:file:media_service/application.yml"
        );
        SpringApplication.run(MediaServiceApplication.class, args);
    }
}
