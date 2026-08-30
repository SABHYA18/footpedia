package com.footpedia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "footpedia.providers.api-football")
public record ApiFootballProperties(
        String baseUrl, String apiKey, int requestsPerDay, int requestsPerMinute, int reserveThreshold) {
}
