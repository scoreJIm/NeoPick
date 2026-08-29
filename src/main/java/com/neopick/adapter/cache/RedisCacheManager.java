package com.neopick.adapter.cache;

import com.neopick.port.cache.CacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisCacheManager implements CacheManager {

    private final StringRedisTemplate redisTemplate;

    public RedisCacheManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public long increment(String key, Duration ttl) {
        Long value = redisTemplate.opsForValue().increment(key);
        if (value != null && value == 1) {
            redisTemplate.expire(key, ttl.toSeconds(), TimeUnit.SECONDS);
        }
        return value != null ? value : 0;
    }

    @Override
    public void setAdd(String key, String member, Duration ttl) {
        redisTemplate.opsForSet().add(key, member);
        redisTemplate.expire(key, ttl);
    }

    @Override
    public void setRemove(String key, String member) {
        redisTemplate.opsForSet().remove(key, member);
    }
}
