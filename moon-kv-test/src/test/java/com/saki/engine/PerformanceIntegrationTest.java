package com.saki.engine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PerformanceIntegrationTest {

    @BeforeEach
    public void setUp() {
        System.setProperty("kv.wal.path", "./data/test_" + System.currentTimeMillis() + ".wal");
    }

    @AfterEach
    public void tearDown() {
        KVStore store = KVStore.getInstance();
        if (store != null) {
            store.close();
        }
        KVStore.resetInstance();
    }

    @Test
    public void testBasicOperations() {
        KVStore.set("test-key", "test-value");
        String value = KVStore.get("test-key");
        assertEquals("test-value", value);
        
        KVStore.delete("test-key");
        value = KVStore.get("test-key");
        assertNull(value);
    }

    @Test
    public void testSetexOperation() {
        KVStore.setex("test-key-ttl", "test-value", 3600);
        String value = KVStore.get("test-key-ttl");
        assertEquals("test-value", value);
    }

    @Test
    public void testConcurrentOperations() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 100;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String key = "concurrent-key-" + threadId + "-" + j;
                    String value = "concurrent-value-" + threadId + "-" + j;
                    KVStore.set(key, value);
                    KVStore.get(key);
                    KVStore.delete(key);
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
    }
}
