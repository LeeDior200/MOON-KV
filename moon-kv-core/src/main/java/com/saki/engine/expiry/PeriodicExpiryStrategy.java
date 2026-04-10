package com.saki.engine.expiry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class PeriodicExpiryStrategy implements ExpiryStrategy {
    private static final Logger logger = LoggerFactory.getLogger(PeriodicExpiryStrategy.class);
    
    private final ConcurrentHashMap<String, Long> expireAt;
    private final ConcurrentHashMap<String, String> store;
    private final BiConsumer<String, String> onExpire;
    private final long scanIntervalMs;
    private final int scanBatchSize;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    public PeriodicExpiryStrategy(ConcurrentHashMap<String, Long> expireAt,
                                  ConcurrentHashMap<String, String> store,
                                  BiConsumer<String, String> onExpire,
                                  ExpiryConfig config) {
        this.expireAt = expireAt;
        this.store = store;
        this.onExpire = onExpire;
        this.scanIntervalMs = config.getScanIntervalMs();
        this.scanBatchSize = config.getScanBatchSize();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Expiry-Scanner");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void start() {
        running = true;
        scheduler.scheduleAtFixedRate(this::scanExpiredKeys, 
            scanIntervalMs, scanIntervalMs, TimeUnit.MILLISECONDS);
        logger.info("PeriodicExpiryStrategy started with interval {} ms", scanIntervalMs);
    }

    @Override
    public void stop() {
        running = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("PeriodicExpiryStrategy stopped");
    }

    @Override
    public void onAccess(String key) {
        // Periodic strategy doesn't need to do anything on access
    }

    private void scanExpiredKeys() {
        if (!running) {
            return;
        }

        long now = System.currentTimeMillis();
        int count = 0;
        
        for (Map.Entry<String, Long> entry : expireAt.entrySet()) {
            if (!running) {
                break;
            }
            
            if (count >= scanBatchSize) {
                break;
            }
            
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
        
        if (count > 0) {
            logger.debug("Periodic scan cleaned {} expired keys", count);
        }
    }
}
