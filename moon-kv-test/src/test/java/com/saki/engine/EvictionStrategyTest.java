package com.saki.engine;

import com.saki.engine.expiry.ExpiryConfig;
import com.saki.engine.memory.EvictionStrategyType;
import com.saki.engine.memory.MemoryConfig;
import com.saki.wal.config.WalConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EvictionStrategyTest {

    @TempDir
    Path tempDir;

    private String testWalPath;

    @BeforeEach
    void setUp() {
        testWalPath = tempDir.resolve("test_eviction.wal").toString();
        System.setProperty("kv.wal.path", testWalPath);
    }

    @AfterEach
    void tearDown() {
        try {
            KVStore store = KVStore.getInstance();
            if (store != null) {
                store.close();
            }
            KVStore.resetInstance();
        } catch (Exception e) {
        }
    }

    @Test
    void testLRUEviction() {
        MemoryConfig memoryConfig = new MemoryConfig();
        memoryConfig.setEvictionStrategy(EvictionStrategyType.LRU);
        memoryConfig.setMaxSizeMB(1);
        memoryConfig.setEvictionRatio(0.8);
        
        KVStore store = KVStore.getInstance(new WalConfig(), new ExpiryConfig(), memoryConfig);
        
        store.set("lru_key_1", "value_1");
        store.set("lru_key_2", "value_2");
        store.set("lru_key_3", "value_3");
        
        store.get("lru_key_1");
        store.get("lru_key_2");
        
        for (int i = 0; i < 5000; i++) {
            store.set("lru_fill_" + i, "x".repeat(200));
        }
        
        var stats = store.getMemoryStats();
        assertTrue(stats.getUsageRatio() <= 0.9, 
            "Memory usage should be controlled after eviction: " + stats.getUsageRatio());
    }

    @Test
    void testLRUEvictionOrder() {
        MemoryConfig memoryConfig = new MemoryConfig();
        memoryConfig.setEvictionStrategy(EvictionStrategyType.LRU);
        memoryConfig.setMaxSizeMB(1);
        memoryConfig.setEvictionRatio(0.7);
        
        KVStore store = KVStore.getInstance(new WalConfig(), new ExpiryConfig(), memoryConfig);
        
        store.set("lru_order_1", "value_1");
        store.set("lru_order_2", "value_2");
        store.set("lru_order_3", "value_3");
        
        store.get("lru_order_1");
        store.get("lru_order_2");
        
        for (int i = 0; i < 3000; i++) {
            store.set("lru_fill_" + i, "x".repeat(200));
        }
        
        assertTrue(store.get("lru_order_1") != null || store.get("lru_order_2") != null,
            "Recently accessed keys should be preserved longer");
    }

    @Test
    void testLFUEviction() {
        MemoryConfig memoryConfig = new MemoryConfig();
        memoryConfig.setEvictionStrategy(EvictionStrategyType.LFU);
        memoryConfig.setMaxSizeMB(1);
        memoryConfig.setEvictionRatio(0.8);
        
        KVStore store = KVStore.getInstance(new WalConfig(), new ExpiryConfig(), memoryConfig);
        
        store.set("lfu_key_1", "value_1");
        store.set("lfu_key_2", "value_2");
        store.set("lfu_key_3", "value_3");
        
        for (int i = 0; i < 10; i++) {
            store.get("lfu_key_1");
            store.get("lfu_key_2");
        }
        
        for (int i = 0; i < 5000; i++) {
            store.set("lfu_fill_" + i, "x".repeat(200));
        }
        
        var stats = store.getMemoryStats();
        assertTrue(stats.getUsageRatio() <= 0.9,
            "Memory usage should be controlled after eviction: " + stats.getUsageRatio());
    }

    @Test
    void testLFUEvictionOrder() {
        MemoryConfig memoryConfig = new MemoryConfig();
        memoryConfig.setEvictionStrategy(EvictionStrategyType.LFU);
        memoryConfig.setMaxSizeMB(1);
        memoryConfig.setEvictionRatio(0.7);
        
        KVStore store = KVStore.getInstance(new WalConfig(), new ExpiryConfig(), memoryConfig);
        
        store.set("lfu_order_1", "value_1");
        store.set("lfu_order_2", "value_2");
        store.set("lfu_order_3", "value_3");
        
        for (int i = 0; i < 20; i++) {
            store.get("lfu_order_1");
            store.get("lfu_order_2");
        }
        store.get("lfu_order_3");
        
        for (int i = 0; i < 3000; i++) {
            store.set("lfu_fill_" + i, "x".repeat(200));
        }
        
        assertTrue(store.get("lfu_order_1") != null || store.get("lfu_order_2") != null,
            "Frequently accessed keys should be preserved longer");
    }

    @Test
    void testFIFOEviction() {
        MemoryConfig memoryConfig = new MemoryConfig();
        memoryConfig.setEvictionStrategy(EvictionStrategyType.FIFO);
        memoryConfig.setMaxSizeMB(1);
        memoryConfig.setEvictionRatio(0.8);
        
        KVStore store = KVStore.getInstance(new WalConfig(), new ExpiryConfig(), memoryConfig);
        
        store.set("fifo_key_1", "value_1");
        store.set("fifo_key_2", "value_2");
        store.set("fifo_key_3", "value_3");
        
        for (int i = 0; i < 5000; i++) {
            store.set("fifo_fill_" + i, "x".repeat(200));
        }
        
        var stats = store.getMemoryStats();
        assertTrue(stats.getUsageRatio() <= 0.9,
            "Memory usage should be controlled after eviction: " + stats.getUsageRatio());
    }

    @Test
    void testFIFOEvictionOrder() {
        MemoryConfig memoryConfig = new MemoryConfig();
        memoryConfig.setEvictionStrategy(EvictionStrategyType.FIFO);
        memoryConfig.setMaxSizeMB(1);
        memoryConfig.setEvictionRatio(0.7);
        
        KVStore store = KVStore.getInstance(new WalConfig(), new ExpiryConfig(), memoryConfig);
        
        store.set("fifo_order_1", "value_1");
        store.set("fifo_order_2", "value_2");
        store.set("fifo_order_3", "value_3");
        
        store.get("fifo_order_1");
        store.get("fifo_order_2");
        
        for (int i = 0; i < 3000; i++) {
            store.set("fifo_fill_" + i, "x".repeat(200));
        }
        
        assertTrue(store.get("fifo_order_3") != null,
            "FIFO should preserve later inserted keys when earlier ones are evicted");
    }

    @Test
    void testEvictionRatio() {
        MemoryConfig memoryConfig = new MemoryConfig();
        memoryConfig.setEvictionStrategy(EvictionStrategyType.LRU);
        memoryConfig.setMaxSizeMB(1);
        memoryConfig.setEvictionRatio(0.5);
        
        KVStore store = KVStore.getInstance(new WalConfig(), new ExpiryConfig(), memoryConfig);
        
        for (int i = 0; i < 10000; i++) {
            store.set("ratio_key_" + i, "x".repeat(100));
        }
        
        var stats = store.getMemoryStats();
        assertTrue(stats.getUsageRatio() <= 0.7,
            "Memory usage should respect eviction ratio: " + stats.getUsageRatio());
    }

    @Test
    void testMemoryStatsAccuracy() {
        MemoryConfig memoryConfig = new MemoryConfig();
        memoryConfig.setEvictionStrategy(EvictionStrategyType.LRU);
        memoryConfig.setMaxSizeMB(10);
        
        KVStore store = KVStore.getInstance(new WalConfig(), new ExpiryConfig(), memoryConfig);
        
        var initialStats = store.getMemoryStats();
        assertEquals(0, initialStats.getEntryCount());
        
        store.set("stats_key_1", "value_1");
        store.set("stats_key_2", "value_2");
        
        var stats = store.getMemoryStats();
        assertEquals(2, stats.getEntryCount());
        assertTrue(stats.getUsedBytes() > 0);
        
        store.delete("stats_key_1");
        
        stats = store.getMemoryStats();
        assertEquals(1, stats.getEntryCount());
    }

    @Test
    void testManualEviction() {
        MemoryConfig memoryConfig = new MemoryConfig();
        memoryConfig.setEvictionStrategy(EvictionStrategyType.LRU);
        memoryConfig.setMaxSizeMB(10);
        memoryConfig.setEvictionRatio(0.8);
        
        KVStore store = KVStore.getInstance(new WalConfig(), new ExpiryConfig(), memoryConfig);
        
        for (int i = 0; i < 1000; i++) {
            store.set("manual_key_" + i, "value_" + i);
        }
        
        var beforeEviction = store.getMemoryStats();
        store.evict();
        var afterEviction = store.getMemoryStats();
        
        assertTrue(afterEviction.getEntryCount() <= beforeEviction.getEntryCount(),
            "Entry count should decrease or stay same after eviction");
    }

    @Test
    void testEvictionWithDifferentValueSizes() {
        MemoryConfig memoryConfig = new MemoryConfig();
        memoryConfig.setEvictionStrategy(EvictionStrategyType.LRU);
        memoryConfig.setMaxSizeMB(1);
        memoryConfig.setEvictionRatio(0.8);
        
        KVStore store = KVStore.getInstance(new WalConfig(), new ExpiryConfig(), memoryConfig);
        
        store.set("small_key", "small_value");
        store.set("medium_key", "x".repeat(1000));
        store.set("large_key", "x".repeat(10000));
        
        for (int i = 0; i < 100; i++) {
            store.set("fill_key_" + i, "x".repeat(5000));
        }
        
        var stats = store.getMemoryStats();
        assertTrue(stats.getUsageRatio() <= 0.9,
            "Memory should be managed with varying value sizes");
    }

    @Test
    void testEvictionWithConcurrentAccess() throws InterruptedException {
        MemoryConfig memoryConfig = new MemoryConfig();
        memoryConfig.setEvictionStrategy(EvictionStrategyType.LRU);
        memoryConfig.setMaxSizeMB(5);
        memoryConfig.setEvictionRatio(0.8);
        
        KVStore store = KVStore.getInstance(new WalConfig(), new ExpiryConfig(), memoryConfig);
        
        int threadCount = 10;
        int operationsPerThread = 500;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String key = "concurrent_evict_" + threadId + "_" + j;
                    store.set(key, "x".repeat(100));
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        var stats = store.getMemoryStats();
        assertTrue(stats.getUsageRatio() <= 0.95,
            "Memory should be managed under concurrent access: " + stats.getUsageRatio());
    }

    @Test
    void testNoEvictionWhenBelowLimit() {
        MemoryConfig memoryConfig = new MemoryConfig();
        memoryConfig.setEvictionStrategy(EvictionStrategyType.LRU);
        memoryConfig.setMaxSizeMB(100);
        memoryConfig.setEvictionRatio(0.8);
        
        KVStore store = KVStore.getInstance(new WalConfig(), new ExpiryConfig(), memoryConfig);
        
        for (int i = 0; i < 100; i++) {
            store.set("no_evict_" + i, "value_" + i);
        }
        
        var stats = store.getMemoryStats();
        assertTrue(stats.getUsageRatio() < 0.8,
            "No eviction should occur when below limit");
        
        for (int i = 0; i < 100; i++) {
            assertEquals("value_" + i, store.get("no_evict_" + i),
                "All keys should be preserved when no eviction needed");
        }
    }
}
