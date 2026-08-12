package com.discordclone.apigateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class CorsPreflightFilter implements WebFilter, Ordered {

    private static final String ALLOWED_METHODS = "GET,POST,PUT,PATCH,DELETE,OPTIONS";
    private final List<String> allowedOriginPatterns;

    public CorsPreflightFilter(
            @Value("${cors.allowed-origin-patterns:${CORS_ALLOWED_ORIGIN_PATTERNS:http://localhost:3000}}")
            String allowedOriginPatterns
    ) {
        this.allowedOriginPatterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(pattern -> !pattern.isBlank())
                .toList();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (!HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }

        String origin = request.getHeaders().getOrigin();
        ServerHttpResponse response = exchange.getResponse();

        if (isAllowedOrigin(origin)) {
            HttpHeaders headers = response.getHeaders();
            headers.setAccessControlAllowOrigin(origin);
            headers.setAccessControlAllowCredentials(true);
            headers.add(HttpHeaders.VARY, HttpHeaders.ORIGIN);
            headers.add(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
            headers.add(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
            headers.add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, ALLOWED_METHODS);
            headers.add(
                    HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                    request.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS) != null
                            ? request.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS)
                            : "Authorization,Content-Type"
            );
            headers.add(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
            response.setStatusCode(HttpStatus.OK);
            return response.setComplete();
        }

        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }

    private boolean isAllowedOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            return false;
        }
        return allowedOriginPatterns.stream()
                .anyMatch(pattern -> "*".equals(pattern) || PatternMatchUtils.simpleMatch(pattern, origin));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
