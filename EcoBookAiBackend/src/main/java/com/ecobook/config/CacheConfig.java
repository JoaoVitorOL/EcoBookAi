package com.ecobook.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Runtime cache configuration for hot authenticated reads.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                buildCache(CacheNames.USER_PROFILE, Duration.ofMinutes(30), 1_000),
                buildCache(CacheNames.USER_CONSENT_STATUS, Duration.ofMinutes(30), 1_000),
                buildCache(CacheNames.USER_AUTH_CONTEXT, Duration.ofMinutes(30), 1_000),
                buildCache(CacheNames.REFERENCE_DATA_MATERIAL_OPTIONS, Duration.ofHours(12), 10)
        ));
        return cacheManager;
    }

    private CaffeineCache buildCache(String name, Duration ttl, long maximumSize) {
        return new CaffeineCache(
                name,
                Caffeine.newBuilder()
                        .expireAfterWrite(ttl)
                        .maximumSize(maximumSize)
                        .recordStats()
                        .build()
        );
    }
}
