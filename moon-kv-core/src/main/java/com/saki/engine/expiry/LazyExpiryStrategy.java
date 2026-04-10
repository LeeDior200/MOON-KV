package com.saki.engine.expiry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class LazyExpiryStrategy implements ExpiryStrategy {
    private static final Logger logger = LoggerFactory.getLogger(LazyExpiryStrategy.class);
    
    private final ConcurrentHashMap<String, Long> expireAt;
    private final ConcurrentHashMap<String, String> store;
    private final BiConsumer<String, String> onExpire;

    public LazyExpiryStrategy(ConcurrentHashMap<String, Long> expireAt,
                             ConcurrentHashMap<String, String> store,
                             BiConsumer<String, String> onExpire) {
        this.expireAt = expireAt;
        this.store = store;
        this.onExpire = onExpire;
    }

    @Override
    public void start() {
        logger.info("LazyExpiryStrategy started");
    }

    @Override
    public void stop() {
        logger.info("LazyExpiryStrategy stopped");
    }

    @Override
    public void onAccess(String key) {
        Long expire = expireAt.get(key);
        if (expire != null && System.currentTimeMillis() > expire) {
            String value = store.get(key);
            store.remove(key);
            expireAt.remove(key);
            if (onExpire != null) {
                onExpire.accept(key, value);
            }
            logger.debug("Lazy expired key: {}", key);
        }
    }
}
