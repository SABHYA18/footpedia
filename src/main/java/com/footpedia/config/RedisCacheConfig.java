package com.footpedia.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Map;

/**
 * Cache-aside over Redis: each cache name gets its own TTL, pulled from the
 * table in {@code footpedia.cache-ttl} rather than one blanket duration,
 * because live scores and historical fixtures go stale at wildly different
 * rates.
 */
@Configuration
@EnableConfigurationProperties(CacheTtlProperties.class)
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, CacheTtlProperties ttl) {
        // GenericJackson2JsonRedisSerializer's own default ObjectMapper has no
        // JSR-310 module, so Instant fields (kickoff times) fail to serialize
        // unless we hand it one explicitly. And passing a mapper via this
        // constructor (rather than the no-arg one) skips Spring Data Redis's
        // own default-typing setup, so without activating it ourselves,
        // deserialization has no type hints and reads our records back as
        // plain LinkedHashMaps -- DefaultTyping.EVERYTHING (not NON_FINAL) is
        // required because records are final, which NON_FINAL would skip.
        ObjectMapper redisObjectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder().allowIfSubType(Object.class).build(),
                        ObjectMapper.DefaultTyping.EVERYTHING,
                        JsonTypeInfo.As.PROPERTY);

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(redisObjectMapper)));

        Map<String, RedisCacheConfiguration> perCache = Map.of(
                CacheNames.LIVE_SCORE, base.entryTtl(ttl.liveScore()),
                CacheNames.TODAYS_FIXTURES, base.entryTtl(ttl.todaysFixtures()),
                CacheNames.FIXTURE_LIST, base.entryTtl(ttl.fixtureList()),
                CacheNames.STANDINGS, base.entryTtl(ttl.standings()),
                CacheNames.PLAYER_SEASON_STATS, base.entryTtl(ttl.playerSeasonStats()),
                CacheNames.TRANSFERS, base.entryTtl(ttl.transfers()),
                CacheNames.HISTORICAL_FIXTURE, base.entryTtl(ttl.historicalFixture()));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base)
                .withInitialCacheConfigurations(perCache)
                .build();
    }
}
