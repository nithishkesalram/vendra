package com.procureai.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "procureai.jwt")
public record JwtProperties(
        String secret,
        long accessTokenMinutes,
        long refreshTokenDays
) {
}
