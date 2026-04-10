package com.saki.engine;

import com.saki.engine.expiry.ExpiryConfig;
import com.saki.engine.memory.MemoryConfig;
import com.saki.wal.config.WalConfig;
import com.saki.wal.strategy.FlushStrategyType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.Random.class)
class PersistenceTest {

    @TempDir
    Path tempDir;

    private String testWalPath;
    private static AtomicInteger testCounter = new AtomicInteger(0);

    @BeforeEach
    void setUp() {
        try {
            KVStore store = KVStore.getInstance();
            if (store != null) {
                store.close();
            }
            KVStore.resetInstance();
        } catch (Exception e) {
        }
        
        testWalPath = tempDir.resolve("test_persistence_" + testCounter.incrementAndGet() + ".wal").toString();
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
    void testNormalRecovery() {
        KVStore store = KVStore.getInstance();
        
        store.set("key1", "value1");
        store.set("key2", "value2");
        store.set("key3", "value3");
        store.update("key1", "updated_value1");
        store.delete("key2");
        
        store.close();
        KVStore.resetInstance();
        
        KVStore recovered = KVStore.getInstance();
        
        assertEquals("updated_value1", recovered.get("key1"));
        assertNull(recovered.get("key2"));
        assertEquals("value3", recovered.get("key3"));
    }

    @Test
    void testSetexRecovery() throws InterruptedException {
        KVStore store = KVStore.getInstance();
        
        store.setex("ttl_key1", "value1", 60);
        store.setex("ttl_key2", "value2", 3600);
        
        store.close();
        KVStore.resetInstance();
        
        KVStore recovered = KVStore.getInstance();
        
        assertEquals("value1", recovered.get("ttl_key1"));
        assertEquals("value2", recovered.get("ttl_key2"));
    }

    @Test
    void testLargeDataRecovery() {
        KVStore store = KVStore.getInstance();
        
        int dataSize = 1000;
        Map<String, String> expectedData = new HashMap<>();
        
        for (int i = 0; i < dataSize; i++) {
            String key = "large_key_" + i;
            String value = "large_value_" + i + "_" + "x".repeat(50);
            store.set(key, value);
            expectedData.put(key, value);
        }
        
        store.close();
        KVStore.resetInstance();
        
        KVStore recovered = KVStore.getInstance();
        
        for (Map.Entry<String, String> entry : expectedData.entrySet()) {
            assertEquals(entry.getValue(), recovered.get(entry.getKey()),
                "Data mismatch for key: " + entry.getKey());
        }
    }

    @Test
    void testRecoveryPerformance() {
        KVStore store = KVStore.getInstance();
        
        int dataSize = 10000;
        long writeStart = System.currentTimeMillis();
        
        for (int i = 0; i < dataSize; i++) {
            store.set("perf_key_" + i, "perf_value_" + i);
        }
        
        long writeEnd = System.currentTimeMillis();
        
        store.close();
        KVStore.resetInstance();
        
        long recoverStart = System.currentTimeMillis();
        KVStore recovered = KVStore.getInstance();
        long recoverEnd = System.currentTimeMillis();
        
        long writeTime = writeEnd - writeStart;
        long recoverTime = recoverEnd - recoverStart;
        
        System.out.println("Write " + dataSize + " records: " + writeTime + "ms");
        System.out.println("Recover " + dataSize + " records: " + recoverTime + "ms");
        
        assertTrue(recoverTime < 10000, "Recovery should complete within 10 seconds for " + dataSize + " records");
        
        assertEquals("perf_value_0", recovered.get("perf_key_0"));
        assertEquals("perf_value_9999", recovered.get("perf_key_9999"));
    }

    @Test
    void testWalFileIntegrity() throws IOException {
        KVStore store = KVStore.getInstance();
        
        store.set("integrity_key1", "value1");
        store.set("integrity_key2", "value2");
        store.delete("integrity_key1");
        
        store.close();
        KVStore.resetInstance();
        
        File walFile = new File(testWalPath);
        assertTrue(walFile.exists(), "WAL file should exist");
        assertTrue(walFile.length() > 0, "WAL file should not be empty");
        
        try (BufferedReader reader = new BufferedReader(new FileReader(walFile))) {
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                String[] parts = line.split("\\|");
                assertTrue(parts.length >= 2, "Each line should have at least operation and key");
                assertTrue(parts[0].matches("SET|DEL|UPD|SETEX"), 
                    "Operation should be valid: " + parts[0]);
            }
            assertTrue(lineCount >= 3, "Should have at least 3 operations logged");
        }
    }

    @Test
    void testSyncFlushRecovery() {
        WalConfig walConfig = new WalConfig();
        walConfig.setFlushStrategyType(FlushStrategyType.SYNC);
        
        KVStore store = KVStore.getInstance(walConfig, new ExpiryConfig(), new MemoryConfig());
        
        store.set("sync_key1", "value1");
        store.set("sync_key2", "value2");
        
        store.close();
        KVStore.resetInstance();
        
        KVStore recovered = KVStore.getInstance();
        
        assertEquals("value1", recovered.get("sync_key1"));
        assertEquals("value2", recovered.get("sync_key2"));
    }

    @Test
    void testAsyncFlushRecovery() throws InterruptedException {
        WalConfig walConfig = new WalConfig();
        walConfig.setFlushStrategyType(FlushStrategyType.ASYNC);
        walConfig.setFlushIntervalMs(100);
        
        KVStore store = KVStore.getInstance(walConfig, new ExpiryConfig(), new MemoryConfig());
        
        store.set("async_key1", "value1");
        store.set("async_key2", "value2");
        
        Thread.sleep(200);
        
        store.close();
        KVStore.resetInstance();
        
        KVStore recovered = KVStore.getInstance();
        
        assertEquals("value1", recovered.get("async_key1"));
        assertEquals("value2", recovered.get("async_key2"));
    }

    @Test
    void testBatchFlushRecovery() {
        WalConfig walConfig = new WalConfig();
        walConfig.setFlushStrategyType(FlushStrategyType.BATCH);
        walConfig.setBatchSize(10);
        
        KVStore store = KVStore.getInstance(walConfig, new ExpiryConfig(), new MemoryConfig());
        
        for (int i = 0; i < 15; i++) {
            store.set("batch_key_" + i, "value_" + i);
        }
        
        store.close();
        KVStore.resetInstance();
        
        KVStore recovered = KVStore.getInstance();
        
        for (int i = 0; i < 15; i++) {
            assertEquals("value_" + i, recovered.get("batch_key_" + i),
                "Data mismatch for batch_key_" + i);
        }
    }

    @Test
    void testEmptyWalRecovery() {
        File walFile = new File(testWalPath);
        assertFalse(walFile.exists(), "WAL file should not exist initially");
        
        KVStore store = KVStore.getInstance();
        
        assertNull(store.get("nonexistent_key"));
        
        var stats = store.getMemoryStats();
        assertEquals(0, stats.getEntryCount());
    }

    @Test
    void testCorruptedWalHandling() throws IOException {
        File walFile = new File(testWalPath);
        walFile.getParentFile().mkdirs();
        
        try (FileWriter writer = new FileWriter(walFile)) {
            writer.write("INVALID_LINE_WITHOUT_PIPE\n");
            writer.write("SET|valid_key|valid_value\n");
            writer.write("ANOTHER_INVALID_LINE\n");
        }
        
        KVStore store = KVStore.getInstance();
        
        assertEquals("valid_value", store.get("valid_key"));
    }

    @Test
    void testRecoveryWithUpdateOperations() {
        KVStore store = KVStore.getInstance();
        
        store.set("update_key", "original_value");
        store.update("update_key", "updated_value_1");
        store.update("update_key", "updated_value_2");
        
        store.close();
        KVStore.resetInstance();
        
        KVStore recovered = KVStore.getInstance();
        
        assertEquals("updated_value_2", recovered.get("update_key"));
    }

    @Test
    void testRecoveryWithMixedOperations() {
        KVStore store = KVStore.getInstance();
        
        store.set("key1", "value1");
        store.set("key2", "value2");
        store.delete("key1");
        store.set("key3", "value3");
        store.update("key2", "updated_value2");
        store.setex("key4", "value4", 3600);
        store.delete("key3");
        
        store.close();
        KVStore.resetInstance();
        
        KVStore recovered = KVStore.getInstance();
        
        assertNull(recovered.get("key1"));
        assertEquals("updated_value2", recovered.get("key2"));
        assertNull(recovered.get("key3"));
        assertEquals("value4", recovered.get("key4"));
    }
}
