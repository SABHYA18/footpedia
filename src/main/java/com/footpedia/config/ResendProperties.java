package com.footpedia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "footpedia.resend")
public record ResendProperties(String baseUrl, String apiKey) {
}
