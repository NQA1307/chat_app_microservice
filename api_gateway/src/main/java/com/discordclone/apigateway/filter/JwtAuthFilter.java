package com.discordclone.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;

/**
 * JWT Filter chạy trước khi request được forward tới downstream service.
 * Nếu token hợp lệ → thêm X-User-Id và X-User-Username vào header.
 * Nếu không có token / token lỗi → trả về 401.
 */
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    @Value("${jwt.secret}")
    private String secret;

    public JwtAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest()
                    .getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                Key key = Keys.hmacShaKeyFor(secret.getBytes());
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                // Forward user info tới downstream services qua headers
                String userId = claims.get("userId", Long.class).toString();
                String email  = claims.getSubject();
                String role   = claims.get("role", String.class);

                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(r -> r
                            .header("X-User-Id", userId)
                            .header("X-User-Email", email)
                            .header("X-User-Role", role)
                        ).build();

                return chain.filter(mutatedExchange);

            } catch (Exception e) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    public static class Config {

    }
}
