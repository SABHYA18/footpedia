package com.footpedia.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties({FootballDataProperties.class, ApiFootballProperties.class})
public class ProviderClientConfig {

    // A full-season /matches response comfortably exceeds WebClient's 256KB
    // default in-memory buffer.
    private static final int MAX_RESPONSE_BYTES = 4 * 1024 * 1024;

    @Bean
    public WebClient footballDataWebClient(FootballDataProperties properties) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(MAX_RESPONSE_BYTES))
                .build();

        return WebClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("X-Auth-Token", properties.apiKey())
                .exchangeStrategies(strategies)
                .build();
    }

    @Bean
    public WebClient apiFootballWebClient(ApiFootballProperties properties) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(MAX_RESPONSE_BYTES))
                .build();

        return WebClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("x-apisports-key", properties.apiKey())
                .exchangeStrategies(strategies)
                .build();
    }
}
