package com.footpedia.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "footpedia.mail")
public record MailProperties(
        String from, String frontendUrl, long resetTokenExpiryMinutes, long changeCodeExpiryMinutes) {
}
