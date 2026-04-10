package com.saki.engine.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class FIFOMemoryManager implements MemoryManager {
    private static final Logger logger = LoggerFactory.getLogger(FIFOMemoryManager.class);
    
    private final ConcurrentHashMap<String, String> store;
    private final MemoryConfig config;
    private final AtomicLong usedBytes = new AtomicLong(0);
    private final Queue<String> insertionOrder;

    public FIFOMemoryManager(ConcurrentHashMap<String, String> store, MemoryConfig config) {
        this.store = store;
        this.config = config;
        this.insertionOrder = new LinkedList<>();
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
        synchronized (insertionOrder) {
            insertionOrder.offer(key);
        }
        logger.debug("Memory used: {} bytes", usedBytes.get());
    }

    @Override
    public void onRemove(String key, String value) {
        long size = estimateSize(key, value);
        usedBytes.addAndGet(-size);
        synchronized (insertionOrder) {
            insertionOrder.remove(key);
        }
        logger.debug("Memory used: {} bytes", usedBytes.get());
    }

    @Override
    public void onUpdate(String key, String oldValue, String newValue) {
        long oldSize = estimateSize(key, oldValue);
        long newSize = estimateSize(key, newValue);
        usedBytes.addAndGet(newSize - oldSize);
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

        int evictCount = (int) (store.size() * 0.1);
        logger.info("Starting FIFO eviction, evicting {} entries", evictCount);

        int count = 0;
        synchronized (insertionOrder) {
            while (count < evictCount && !insertionOrder.isEmpty()) {
                String key = insertionOrder.poll();
                if (key != null) {
                    String value = store.remove(key);
                    if (value != null) {
                        long size = estimateSize(key, value);
                        usedBytes.addAndGet(-size);
                        count++;
                    }
                }
            }
        }

        logger.info("FIFO eviction completed, evicted {} entries", count);
    }

    private long estimateSize(String key, String value) {
        if (key == null || value == null) {
            return 0;
        }
        return key.length() * 2L + value.length() * 2L + 64;
    }
}
