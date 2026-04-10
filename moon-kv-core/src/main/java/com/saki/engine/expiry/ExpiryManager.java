package com.saki.engine.expiry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class ExpiryManager {
    private static final Logger logger = LoggerFactory.getLogger(ExpiryManager.class);
    
    private final ConcurrentHashMap<String, Long> expireAt;
    private final ConcurrentHashMap<String, String> store;
    private final ExpiryStrategy strategy;
    private final BiConsumer<String, String> onExpire;

    public ExpiryManager(ConcurrentHashMap<String, Long> expireAt, 
                        ConcurrentHashMap<String, String> store,
                        ExpiryConfig config,
                        BiConsumer<String, String> onExpire) {
        this.expireAt = expireAt;
        this.store = store;
        this.onExpire = onExpire;
        this.strategy = createStrategy(config);
        
        logger.info("ExpiryManager initialized with strategy: {}", config.getStrategyType());
    }

    private ExpiryStrategy createStrategy(ExpiryConfig config) {
        switch (config.getStrategyType()) {
            case LAZY:
                return new LazyExpiryStrategy(expireAt, store, onExpire);
            case PERIODIC:
                return new PeriodicExpiryStrategy(expireAt, store, onExpire, config);
            case HYBRID:
                return new HybridExpiryStrategy(expireAt, store, onExpire, config);
            default:
                logger.warn("Unknown expiry strategy: {}, using HYBRID", config.getStrategyType());
                return new HybridExpiryStrategy(expireAt, store, onExpire, config);
        }
    }

    public void start() {
        strategy.start();
        logger.info("ExpiryManager started");
    }

    public void stop() {
        strategy.stop();
        logger.info("ExpiryManager stopped");
    }

    public void onAccess(String key) {
        strategy.onAccess(key);
    }

    public void cleanExpiredKeys() {
        long now = System.currentTimeMillis();
        int count = 0;
        
        for (Map.Entry<String, Long> entry : expireAt.entrySet()) {
            if (now > entry.getValue()) {
                String key = entry.getKey();
                String value = store.get(key);
                store.remove(key);
                expireAt.remove(key);
                if (onExpire != null) {
                    onExpire.accept(key, value);
                }
                count++;
            }
        }
        
        logger.debug("Cleaned {} expired keys", count);
    }
}
