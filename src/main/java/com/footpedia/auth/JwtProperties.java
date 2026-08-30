package com.footpedia.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "footpedia.jwt")
public record JwtProperties(String secret, long expirationMinutes) {
}
