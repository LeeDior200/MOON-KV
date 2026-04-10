package com.saki.engine;

import com.saki.engine.expiry.ExpiryConfig;
import com.saki.engine.expiry.ExpiryStrategyType;
import com.saki.engine.memory.MemoryConfig;
import com.saki.wal.config.WalConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TTLExpiryTest {

    @TempDir
    Path tempDir;

    private String testWalPath;

    @BeforeEach
    void setUp() {
        testWalPath = tempDir.resolve("test_ttl.wal").toString();
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
    void testLazyExpiryOnAccess() throws InterruptedException {
        ExpiryConfig expiryConfig = new ExpiryConfig();
        expiryConfig.setStrategyType(ExpiryStrategyType.LAZY);
        
        KVStore store = KVStore.getInstance(new WalConfig(), expiryConfig, new MemoryConfig());
        
        store.setex("lazy_key_1", "value_1", 1);
        
        Thread.sleep(500);
        assertEquals("value_1", store.get("lazy_key_1"),
            "Key should still exist before expiry");
        
        Thread.sleep(700);
        assertNull(store.get("lazy_key_1"),
            "Key should be expired and removed on access");
    }

    @Test
    void testLazyExpiryNoAccess() throws InterruptedException {
        ExpiryConfig expiryConfig = new ExpiryConfig();
        expiryConfig.setStrategyType(ExpiryStrategyType.LAZY);
        
        KVStore store = KVStore.getInstance(new WalConfig(), expiryConfig, new MemoryConfig());
        
        store.setex("lazy_no_access", "value", 1);
        
        Thread.sleep(1500);
        
        store.cleanExpiredKeys();
        
        assertNull(store.get("lazy_no_access"),
            "Key should be removed after manual cleanup");
    }

    @Test
    void testPeriodicExpiry() throws InterruptedException {
        ExpiryConfig expiryConfig = new ExpiryConfig();
        expiryConfig.setStrategyType(ExpiryStrategyType.PERIODIC);
        expiryConfig.setScanIntervalMs(200);
        
        KVStore store = KVStore.getInstance(new WalConfig(), expiryConfig, new MemoryConfig());
        
        store.setex("periodic_key_1", "value_1", 1);
        store.setex("periodic_key_2", "value_2", 2);
        
        Thread.sleep(300);
        assertNull(store.get("periodic_key_1"),
            "Key should be expired by periodic scan");
        assertEquals("value_2", store.get("periodic_key_2"),
            "Key with longer TTL should still exist");
    }

    @Test
    void testPeriodicExpiryBatch() throws InterruptedException {
        ExpiryConfig expiryConfig = new ExpiryConfig();
        expiryConfig.setStrategyType(ExpiryStrategyType.PERIODIC);
        expiryConfig.setScanIntervalMs(200);
        
        KVStore store = KVStore.getInstance(new WalConfig(), expiryConfig, new MemoryConfig());
        
        for (int i = 0; i < 10; i++) {
            store.setex("batch_key_" + i, "value_" + i, 1);
        }
        
        Thread.sleep(300);
        
        int remaining = 0;
        for (int i = 0; i < 10; i++) {
            if (store.get("batch_key_" + i) != null) {
                remaining++;
            }
        }
        
        assertEquals(0, remaining, "All expired keys should be cleaned");
    }

    @Test
    void testHybridExpiry() throws InterruptedException {
        ExpiryConfig expiryConfig = new ExpiryConfig();
        expiryConfig.setStrategyType(ExpiryStrategyType.HYBRID);
        expiryConfig.setScanIntervalMs(200);
        
        KVStore store = KVStore.getInstance(new WalConfig(), expiryConfig, new MemoryConfig());
        
        store.setex("hybrid_key_1", "value_1", 1);
        store.setex("hybrid_key_2", "value_2", 3);
        
        Thread.sleep(300);
        assertNull(store.get("hybrid_key_1"),
            "Key should be expired by periodic scan or lazy check");
        
        Thread.sleep(100);
        assertEquals("value_2", store.get("hybrid_key_2"),
            "Key with longer TTL should still exist");
    }

    @Test
    void testHybridExpiryMixedAccess() throws InterruptedException {
        ExpiryConfig expiryConfig = new ExpiryConfig();
        expiryConfig.setStrategyType(ExpiryStrategyType.HYBRID);
        expiryConfig.setScanIntervalMs(200);
        
        KVStore store = KVStore.getInstance(new WalConfig(), expiryConfig, new MemoryConfig());
        
        store.setex("hybrid_access_1", "value_1", 1);
        store.setex("hybrid_access_2", "value_2", 1);
        
        Thread.sleep(500);
        assertEquals("value_1", store.get("hybrid_access_1"));
        
        Thread.sleep(600);
        assertNull(store.get("hybrid_access_1"));
        assertNull(store.get("hybrid_access_2"));
    }

    @Test
    void testTTLPrecision() throws InterruptedException {
        ExpiryConfig expiryConfig = new ExpiryConfig();
        expiryConfig.setStrategyType(ExpiryStrategyType.LAZY);
        
        KVStore store = KVStore.getInstance(new WalConfig(), expiryConfig, new MemoryConfig());
        
        long ttlSeconds = 1;
        store.setex("precision_key", "value", ttlSeconds);
        
        Thread.sleep(900);
        assertEquals("value", store.get("precision_key"),
            "Key should exist just before expiry");
        
        Thread.sleep(200);
        assertNull(store.get("precision_key"),
            "Key should be expired after TTL");
    }

    @Test
    void testLongTTL() throws InterruptedException {
        ExpiryConfig expiryConfig = new ExpiryConfig();
        expiryConfig.setStrategyType(ExpiryStrategyType.LAZY);
        
        KVStore store = KVStore.getInstance(new WalConfig(), expiryConfig, new MemoryConfig());
        
        store.setex("long_ttl_key", "value", 3600);
        
        Thread.sleep(500);
        assertEquals("value", store.get("long_ttl_key"),
            "Key with long TTL should still exist");
    }

    @Test
    void testZeroTTL() {
        ExpiryConfig expiryConfig = new ExpiryConfig();
        expiryConfig.setStrategyType(ExpiryStrategyType.LAZY);
        
        KVStore store = KVStore.getInstance(new WalConfig(), expiryConfig, new MemoryConfig());
        
        store.setex("zero_ttl_key", "value", 0);
        
        assertNull(store.get("zero_ttl_key"),
            "Key with zero TTL should be expired immediately");
    }

    @Test
    void testNegativeTTL() {
        ExpiryConfig expiryConfig = new ExpiryConfig();
        expiryConfig.setStrategyType(ExpiryStrategyType.LAZY);
        
        KVStore store = KVStore.getInstance(new WalConfig(), expiryConfig, new MemoryConfig());
        
        store.setex("negative_ttl_key", "value", -1);
        
        assertNull(store.get("negative_ttl_key"),
            "Key with negative TTL should be expired immediately");
    }

    @Test
    void testTTLWithUpdate() throws InterruptedException {
        ExpiryConfig expiryConfig = new ExpiryConfig();
        expiryConfig.setStrategyType(ExpiryStrategyType.LAZY);
        
        KVStore store = KVStore.getInstance(new WalConfig(), expiryConfig, new MemoryConfig());
        
        store.set("update_ttl_key", "original_value");
        store.setex("update_ttl_key", "new_value", 1);
        
        assertEquals("new_value", store.get("update_ttl_key"));
        
        Thread.sleep(1100);
        assertNull(store.get("update_ttl_key"),
            "Updated key with TTL should expire");
    }

    @Test
    void testTTLWithDelete() throws InterruptedException {
        ExpiryConfig expiryConfig = new ExpiryConfig();
        expiryConfig.setStrategyType(ExpiryStrategyType.LAZY);
        
        KVStore store = KVStore.getInstance(new WalConfig(), expiryConfig, new MemoryConfig());
        
        store.setex("delete_ttl_key", "value", 10);
        store.delete("delete_ttl_key");
        
        assertNull(store.get("delete_ttl_key"),
            "Deleted key should not exist");
        
        store.set("delete_ttl_key", "new_value");
        assertEquals("new_value", store.get("delete_ttl_key"),
            "Re-created key should not have TTL");
    }

    @Test
    void testMultipleExpiryStrategies() throws InterruptedException {
        for (ExpiryStrategyType strategyType : ExpiryStrategyType.values()) {
            KVStore.resetInstance();
            
            ExpiryConfig expiryConfig = new ExpiryConfig();
            expiryConfig.setStrategyType(strategyType);
            expiryConfig.setScanIntervalMs(200);
            
            KVStore store = KVStore.getInstance(new WalConfig(), expiryConfig, new MemoryConfig());
            
            store.setex("multi_strategy_" + strategyType, "value", 1);
            
            Thread.sleep(1200);
            
            assertNull(store.get("multi_strategy_" + strategyType),
                "Key should expire with strategy: " + strategyType);
            
            store.close();
        }
    }

    @Test
    void testExpiryWithConcurrentAccess() throws InterruptedException {
        ExpiryConfig expiryConfig = new ExpiryConfig();
        expiryConfig.setStrategyType(ExpiryStrategyType.HYBRID);
        expiryConfig.setScanIntervalMs(100);
        
        KVStore store = KVStore.getInstance(new WalConfig(), expiryConfig, new MemoryConfig());
        
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    store.setex("concurrent_ttl_" + threadId + "_" + j, "value", 5);
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        int count = 0;
        for (int i = 0; i < threadCount; i++) {
            for (int j = 0; j < 10; j++) {
                if (store.get("concurrent_ttl_" + i + "_" + j) != null) {
                    count++;
                }
            }
        }
        
        assertEquals(threadCount * 10, count, "All keys should exist before expiry");
    }

    @Test
    void testManualCleanup() throws InterruptedException {
        ExpiryConfig expiryConfig = new ExpiryConfig();
        expiryConfig.setStrategyType(ExpiryStrategyType.LAZY);
        
        KVStore store = KVStore.getInstance(new WalConfig(), expiryConfig, new MemoryConfig());
        
        for (int i = 0; i < 5; i++) {
            store.setex("manual_cleanup_" + i, "value_" + i, 1);
        }
        
        Thread.sleep(1100);
        
        store.cleanExpiredKeys();
        
        for (int i = 0; i < 5; i++) {
            assertNull(store.get("manual_cleanup_" + i),
                "All expired keys should be cleaned");
        }
    }
}
