package com.discordclone.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class JwtAuthGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthGatewayFilterFactory.Config> {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer:discord-clone}")
    private String issuer;

    @Value("${internal.service-secret}")
    private String internalServiceSecret;

    private final ReactiveStringRedisTemplate redisTemplate;

    public JwtAuthGatewayFilterFactory(ReactiveStringRedisTemplate redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                Key key = Keys.hmacShaKeyFor(secret.getBytes());
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .requireIssuer(issuer)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                if (!"access".equals(claims.get("type", String.class))) {
                    log.warn("JWT rejected because token type is not access");
                    return onError(exchange, HttpStatus.UNAUTHORIZED);
                }

                String userId = claims.getSubject();
                if (userId == null || userId.isBlank()) {
                    log.warn("JWT rejected because subject is missing");
                    return onError(exchange, HttpStatus.UNAUTHORIZED);
                }

                String email = claims.get("email", String.class);
                String role = extractPrimaryRole(claims);
                String username = claims.get("username", String.class);

                return isAccessTokenRevoked(claims, userId)
                        .flatMap(revoked -> {
                            if (revoked) {
                                log.warn("JWT rejected because access token was revoked. userId={}", userId);
                                return onError(exchange, HttpStatus.UNAUTHORIZED);
                            }

                            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                    .headers(headers -> {
                                        headers.remove("X-User-Id");
                                        headers.remove("X-User-Email");
                                        headers.remove("X-User-Role");
                                        headers.remove("X-User-Username");
                                        headers.remove("X-Internal-Secret");
                                    })
                                    .header("X-User-Id", userId)
                                    .header("X-User-Email", email != null ? email : "")
                                    .header("X-User-Role", role != null ? role : "")
                                    .header("X-User-Username", username != null ? username : userId)
                                    .header("X-Internal-Secret", internalServiceSecret)
                                    .build();

                            ServerWebExchange mutatedExchange = exchange.mutate()
                                    .request(mutatedRequest)
                                    .build();

                            log.debug("JWT validated and headers injected for userId={}", userId);
                            return chain.filter(mutatedExchange);
                        });
            } catch (Exception e) {
                log.warn("JWT validation failed: {}", e.getMessage());
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    private String extractPrimaryRole(Claims claims) {
        List<?> roles = claims.get("roles", List.class);
        if (roles != null && !roles.isEmpty()) {
            return roles.get(0).toString();
        }
        return claims.get("role", String.class);
    }

    private Mono<Boolean> isAccessTokenRevoked(Claims claims, String userId) {
        String tokenId = claims.getId();
        Date issuedAt = claims.getIssuedAt();

        Mono<Boolean> tokenRevoked = tokenId == null || tokenId.isBlank()
                ? Mono.just(false)
                : redisTemplate.hasKey("revoked_access_token:" + tokenId);

        Mono<Boolean> userRevoked = redisTemplate.opsForValue()
                .get("access_token_valid_after:" + userId)
                .map(value -> isIssuedBeforeCutoff(issuedAt, value))
                .defaultIfEmpty(false);

        return Mono.zip(tokenRevoked, userRevoked, (singleTokenRevoked, allUserTokensRevoked) ->
                        singleTokenRevoked || allUserTokensRevoked)
                .onErrorReturn(true);
    }

    private boolean isIssuedBeforeCutoff(Date issuedAt, String validAfterValue) {
        if (issuedAt == null || validAfterValue == null || validAfterValue.isBlank()) {
            return false;
        }
        try {
            return issuedAt.getTime() < Long.parseLong(validAfterValue);
        } catch (NumberFormatException e) {
            return true;
        }
    }

    public static class Config {
    }
}
