package com.footpedia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "footpedia.cache-ttl")
public record CacheTtlProperties(
        Duration liveScore,
        Duration todaysFixtures,
        Duration fixtureList,
        Duration standings,
        Duration playerSeasonStats,
        Duration transfers,
        Duration historicalFixture) {
}
