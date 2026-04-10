package com.saki.engine;

import com.saki.engine.expiry.ExpiryConfig;
import com.saki.engine.expiry.ExpiryStrategyType;
import com.saki.engine.memory.EvictionStrategyType;
import com.saki.engine.memory.MemoryConfig;
import com.saki.wal.config.WalConfig;
import com.saki.wal.strategy.FlushStrategyType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class KVStoreP1Test {

    @TempDir
    Path tempDir;

    private String testWalPath;

    @BeforeEach
    void setUp() {
        testWalPath = tempDir.resolve("test_kv_store.wal").toString();
        System.setProperty("kv.wal.path", testWalPath);
    }

    @AfterEach
    void tearDown() {
        KVStore.getInstance().close();
        KVStore.resetInstance();
    }

    @Test
    void testAsyncFlushStrategy() {
        WalConfig walConfig = new WalConfig();
        walConfig.setFlushStrategyType(FlushStrategyType.ASYNC);
        walConfig.setFlushIntervalMs(100);
        
        ExpiryConfig expiryConfig = new ExpiryConfig();
        expiryConfig.setStrategyType(ExpiryStrategyType.LAZY);
        
        MemoryConfig memoryConfig = new MemoryConfig();
        memoryConfig.setEvictionStrategy(EvictionStrategyType.LRU);
        memoryConfig.setMaxSizeMB(10);
        
        KVStore store = KVStore.getInstance(walConfig, expiryConfig, memoryConfig);
        
        store.set("key1", "value1");
        assertEquals("value1", store.get("key1"));
    }

    @Test
    void testBatchFlushStrategy() {
        WalConfig walConfig = new WalConfig();
        walConfig.setFlushStrategyType(FlushStrategyType.BATCH);
        walConfig.setBatchSize(10);
        
        ExpiryConfig expiryConfig = new ExpiryConfig();
        expiryConfig.setStrategyType(ExpiryStrategyType.PERIODIC);
        
        MemoryConfig memoryConfig = new MemoryConfig();
        memoryConfig.setEvictionStrategy(EvictionStrategyType.LFU);
        
        KVStore store = KVStore.getInstance(walConfig, expiryConfig, memoryConfig);
        
        for (int i = 0; i < 15; i++) {
            store.set("key" + i, "value" + i);
        }
        
        assertEquals("value5", store.get("key5"));
    }

    @Test
    void testMemoryStats() {
        KVStore store = KVStore.getInstance();
        
        store.set("key1", "value1");
        store.set("key2", "value2");
        
        var stats = store.getMemoryStats();
        assertNotNull(stats);
        assertTrue(stats.getEntryCount() >= 2);
        assertTrue(stats.getUsedBytes() > 0);
    }

    @Test
    void testHybridExpiryStrategy() throws InterruptedException {
        ExpiryConfig expiryConfig = new ExpiryConfig();
        expiryConfig.setStrategyType(ExpiryStrategyType.HYBRID);
        expiryConfig.setScanIntervalMs(100);
        
        KVStore store = KVStore.getInstance(new WalConfig(), expiryConfig, new MemoryConfig());
        
        store.setex("key1", "value1", 1);
        assertEquals("value1", store.get("key1"));
        
        Thread.sleep(1100);
        
        assertNull(store.get("key1"));
    }

    @Test
    void testMemoryEviction() {
        MemoryConfig memoryConfig = new MemoryConfig();
        memoryConfig.setMaxSizeMB(1);
        memoryConfig.setEvictionStrategy(EvictionStrategyType.LRU);
        memoryConfig.setEvictionRatio(0.5);
        
        KVStore store = KVStore.getInstance(new WalConfig(), new ExpiryConfig(), memoryConfig);
        
        for (int i = 0; i < 10000; i++) {
            store.set("key" + i, "value" + i + "_" + "x".repeat(100));
        }
        
        var stats = store.getMemoryStats();
        assertTrue(stats.getUsageRatio() <= memoryConfig.getEvictionRatio() * 1.5);
    }
}
