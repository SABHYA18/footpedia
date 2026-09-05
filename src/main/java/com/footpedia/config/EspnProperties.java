package com.footpedia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "footpedia.providers.espn")
public record EspnProperties(String baseUrl) {
}
