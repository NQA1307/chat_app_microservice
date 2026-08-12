package com.discordclone.messageservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import com.discordclone.messageservice.security.MessageCryptoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.time.Duration;

@SpringBootApplication(scanBasePackages = {
        "com.discordclone.messageservice",
        "com.discordclone.common"
})
@EnableConfigurationProperties(MessageCryptoProperties.class)
@EnableDiscoveryClient
public class MessageServiceApplication {

    @Bean
    public RestClient.Builder restClientBuilder(
            @Value("${internal.service-secret}") String internalServiceSecret,
            @Value("${clients.server-service.connect-timeout:2s}") Duration connectTimeout,
            @Value("${clients.server-service.read-timeout:3s}") Duration readTimeout
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().set("X-Internal-Secret", internalServiceSecret);
                    return execution.execute(request, body);
                });
    }

    public static void main(String[] args) {
        System.setProperty(
                "spring.config.additional-location",
                "optional:file:application.yml,optional:file:message_service/application.yml"
        );
        SpringApplication.run(MessageServiceApplication.class, args);
    }
}
