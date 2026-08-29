package com.neopick.infrastructure.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return builder -> {
            var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(5))
                    .serializeKeysWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                    .prefixCacheNameWith("neopick:cache:")
                    .disableCachingNullValues();

            builder.cacheDefaults(defaultConfig);

            builder.withCacheConfiguration("homepage",
                    defaultConfig.entryTtl(Duration.ofMinutes(5)));
            builder.withCacheConfiguration("teacherSearch",
                    defaultConfig.entryTtl(Duration.ofMinutes(2)));
            builder.withCacheConfiguration("featuredTeachers",
                    defaultConfig.entryTtl(Duration.ofMinutes(10)));
            builder.withCacheConfiguration("popularTeachers",
                    defaultConfig.entryTtl(Duration.ofMinutes(10)));
            builder.withCacheConfiguration("weeklyRecommendations",
                    defaultConfig.entryTtl(Duration.ofMinutes(30)));
            builder.withCacheConfiguration("cities",
                    defaultConfig.entryTtl(Duration.ofMinutes(30)));
            builder.withCacheConfiguration("categories",
                    defaultConfig.entryTtl(Duration.ofMinutes(30)));
            builder.withCacheConfiguration("teacherDetail",
                    defaultConfig.entryTtl(Duration.ofMinutes(5)));
        };
    }
}
