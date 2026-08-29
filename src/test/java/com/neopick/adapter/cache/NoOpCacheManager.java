package com.neopick.adapter.cache;

import com.neopick.port.cache.CacheManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * No-op fallback CacheManager for the test profile when Redis is not available.
 * Uses an in-memory ConcurrentHashMap for lightweight test scenarios.
 */
@Component
@ConditionalOnMissingBean(StringRedisTemplate.class)
public class NoOpCacheManager implements CacheManager {

    private final ConcurrentMap<String, String> store = new ConcurrentHashMap<>();

    @Override
    public void set(String key, String value, Duration ttl) {
        store.put(key, value);
    }

    @Override
    public Optional<String> get(String key) {
        return Optional.ofNullable(store.get(key));
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }

    @Override
    public boolean exists(String key) {
        return store.containsKey(key);
    }

    @Override
    public long increment(String key, Duration ttl) {
        return store.merge(key, "1", (old, one) ->
                String.valueOf(Long.parseLong(old) + 1)).hashCode() & 0x7FFFFFFF;
    }

    @Override
    public void setAdd(String key, String member, Duration ttl) {
        store.put(key + ":" + member, "1");
    }

    @Override
    public void setRemove(String key, String member) {
        store.remove(key + ":" + member);
    }
}
