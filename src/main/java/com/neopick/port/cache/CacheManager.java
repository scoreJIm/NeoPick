package com.neopick.port.cache;

import java.time.Duration;
import java.util.Optional;

public interface CacheManager {

    void set(String key, String value, Duration ttl);

    Optional<String> get(String key);

    void delete(String key);

    boolean exists(String key);

    long increment(String key, Duration ttl);

    void setAdd(String key, String member, Duration ttl);

    void setRemove(String key, String member);
}
