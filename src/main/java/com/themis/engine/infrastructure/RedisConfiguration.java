package com.themis.engine.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration for Redis caching.
 * Caches Character and Encounter aggregates using deterministic snapshot serializers
 * and synchronizes cache writes with Spring transactions.
 */
@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class RedisConfiguration {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(60);

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(DEFAULT_TTL)
            .disableCachingNullValues();

        RedisCacheConfiguration characterCacheConfig = baseConfig.serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(new CharacterCacheSerializer())
        );

        RedisCacheConfiguration encounterCacheConfig = baseConfig.serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(new EncounterCacheSerializer())
        );

        return RedisCacheManager.builder(connectionFactory)
            .transactionAware()
            .cacheDefaults(baseConfig)
            .withInitialCacheConfigurations(Map.of(
                "characters", characterCacheConfig,
                "encounters", encounterCacheConfig
            ))
            .build();
    }
}
