package com.saki.engine.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class LRUMemoryManager implements MemoryManager {
    private static final Logger logger = LoggerFactory.getLogger(LRUMemoryManager.class);
    
    private final ConcurrentHashMap<String, String> store;
    private final MemoryConfig config;
    private final AtomicLong usedBytes = new AtomicLong(0);
    private final LinkedHashMap<String, Long> accessOrder;

    public LRUMemoryManager(ConcurrentHashMap<String, String> store, MemoryConfig config) {
        this.store = store;
        this.config = config;
        this.accessOrder = new LinkedHashMap<>(16, 0.75f, true);
    }

    @Override
    public boolean canPut(String key, String value) {
        long size = estimateSize(key, value);
        return usedBytes.get() + size <= config.getMaxSizeBytes() * config.getEvictionRatio();
    }

    @Override
    public void onPut(String key, String value) {
        long size = estimateSize(key, value);
        usedBytes.addAndGet(size);
        synchronized (accessOrder) {
            accessOrder.put(key, System.nanoTime());
        }
        logger.debug("Memory used: {} bytes", usedBytes.get());
    }

    @Override
    public void onRemove(String key, String value) {
        long size = estimateSize(key, value);
        usedBytes.addAndGet(-size);
        synchronized (accessOrder) {
            accessOrder.remove(key);
        }
        logger.debug("Memory used: {} bytes", usedBytes.get());
    }

    @Override
    public void onUpdate(String key, String oldValue, String newValue) {
        long oldSize = estimateSize(key, oldValue);
        long newSize = estimateSize(key, newValue);
        usedBytes.addAndGet(newSize - oldSize);
        synchronized (accessOrder) {
            accessOrder.put(key, System.nanoTime());
        }
    }

    @Override
    public MemoryStats getStats() {
        return new MemoryStats(usedBytes.get(), config.getMaxSizeBytes(), store.size());
    }

    @Override
    public void evict() {
        MemoryStats stats = getStats();
        if (stats.getUsageRatio() < config.getEvictionRatio()) {
            return;
        }

        int evictCount = (int) (store.size() * 0.1); // Evict 10% of entries
        logger.info("Starting eviction, evicting {} entries", evictCount);

        synchronized (accessOrder) {
            int count = 0;
            for (Map.Entry<String, Long> entry : accessOrder.entrySet()) {
                if (count >= evictCount) {
                    break;
                }
                String key = entry.getKey();
                String value = store.remove(key);
                if (value != null) {
                    long size = estimateSize(key, value);
                    usedBytes.addAndGet(-size);
                    count++;
                }
            }
            accessOrder.keySet().removeAll(store.keySet());
            accessOrder.putAll(store.keySet().stream()
                .collect(LinkedHashMap::new, (m, k) -> m.put(k, System.nanoTime()), LinkedHashMap::putAll));
        }

        logger.info("Eviction completed, evicted {} entries", evictCount);
    }

    private long estimateSize(String key, String value) {
        if (key == null || value == null) {
            return 0;
        }
        return key.length() * 2L + value.length() * 2L + 64;
    }
}
