package com.saki.engine.expiry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class HybridExpiryStrategy implements ExpiryStrategy {
    private static final Logger logger = LoggerFactory.getLogger(HybridExpiryStrategy.class);
    
    private final LazyExpiryStrategy lazyStrategy;
    private final PeriodicExpiryStrategy periodicStrategy;

    public HybridExpiryStrategy(ConcurrentHashMap<String, Long> expireAt,
                               ConcurrentHashMap<String, String> store,
                               BiConsumer<String, String> onExpire,
                               ExpiryConfig config) {
        this.lazyStrategy = new LazyExpiryStrategy(expireAt, store, onExpire);
        this.periodicStrategy = new PeriodicExpiryStrategy(expireAt, store, onExpire, config);
    }

    @Override
    public void start() {
        lazyStrategy.start();
        periodicStrategy.start();
        logger.info("HybridExpiryStrategy started");
    }

    @Override
    public void stop() {
        lazyStrategy.stop();
        periodicStrategy.stop();
        logger.info("HybridExpiryStrategy stopped");
    }

    @Override
    public void onAccess(String key) {
        lazyStrategy.onAccess(key);
    }
}
