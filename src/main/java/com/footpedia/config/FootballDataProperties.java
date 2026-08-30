package com.footpedia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "footpedia.providers.football-data")
public record FootballDataProperties(String baseUrl, String apiKey, int requestsPerMinute) {
}
