package com.footpedia.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties({FootballDataProperties.class, ApiFootballProperties.class, EspnProperties.class})
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

    // Unofficial endpoint behind Akamai. Verified empirically (curl against
    // the live endpoint): Akamai's edge here 403s a missing UA, a plain tool
    // UA (e.g. "footpedia/1.0"), and any browser-shaped UA alike, but passes
    // an unmodified curl UA. Brittle and undocumented, so this may need
    // revisiting if Akamai's rule changes -- if calls start 403ing, that's
    // the first thing to check.
    @Bean
    public WebClient espnWebClient(EspnProperties properties) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(MAX_RESPONSE_BYTES))
                .build();

        return WebClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("User-Agent", "curl/8.7.1")
                .exchangeStrategies(strategies)
                .build();
    }
}
