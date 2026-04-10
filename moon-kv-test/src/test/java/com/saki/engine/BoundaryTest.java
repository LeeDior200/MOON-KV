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

class BoundaryTest {

    @TempDir
    Path tempDir;

    private String testWalPath;

    @BeforeEach
    void setUp() {
        testWalPath = tempDir.resolve("test_boundary.wal").toString();
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
    void testNullKey() {
        KVStore store = KVStore.getInstance();
        
        store.set(null, "value");
        assertNull(store.get(null));
        
        store.delete(null);
        
        store.update(null, "new_value");
    }

    @Test
    void testNullValue() {
        KVStore store = KVStore.getInstance();
        
        store.set("key", null);
        assertNull(store.get("key"));
        
        store.setex("key_with_ttl", null, 10);
        assertNull(store.get("key_with_ttl"));
    }

    @Test
    void testEmptyStringKey() {
        KVStore store = KVStore.getInstance();
        
        store.set("", "empty_key_value");
        assertEquals("empty_key_value", store.get(""));
        
        store.delete("");
        assertNull(store.get(""));
    }

    @Test
    void testEmptyStringValue() {
        KVStore store = KVStore.getInstance();
        
        store.set("empty_value_key", "");
        assertEquals("", store.get("empty_value_key"));
    }

    @Test
    void testLargeValue_1KB() {
        KVStore store = KVStore.getInstance();
        
        String largeValue = "x".repeat(1024);
        store.set("large_1kb", largeValue);
        
        assertEquals(largeValue, store.get("large_1kb"));
    }

    @Test
    void testLargeValue_10KB() {
        KVStore store = KVStore.getInstance();
        
        String largeValue = "x".repeat(10 * 1024);
        store.set("large_10kb", largeValue);
        
        assertEquals(largeValue, store.get("large_10kb"));
    }

    @Test
    void testLargeValue_100KB() {
        KVStore store = KVStore.getInstance();
        
        String largeValue = "x".repeat(100 * 1024);
        store.set("large_100kb", largeValue);
        
        assertEquals(largeValue, store.get("large_100kb"));
    }

    @Test
    void testLargeValue_1MB() {
        KVStore store = KVStore.getInstance();
        
        String largeValue = "x".repeat(1024 * 1024);
        store.set("large_1mb", largeValue);
        
        assertEquals(largeValue, store.get("large_1mb"));
    }

    @Test
    void testLargeKey() {
        KVStore store = KVStore.getInstance();
        
        String largeKey = "k".repeat(10000);
        store.set(largeKey, "value");
        
        assertEquals("value", store.get(largeKey));
    }

    @Test
    void testUnicodeKey() {
        KVStore store = KVStore.getInstance();
        
        store.set("中文键", "中文值");
        assertEquals("中文值", store.get("中文键"));
        
        store.set("日本語キー", "日本語値");
        assertEquals("日本語値", store.get("日本語キー"));
        
        store.set("한국어키", "한국어값");
        assertEquals("한국어값", store.get("한국어키"));
        
        store.set("🔑🗝️", "emoji_value");
        assertEquals("emoji_value", store.get("🔑🗝️"));
    }

    @Test
    void testUnicodeValue() {
        KVStore store = KVStore.getInstance();
        
        store.set("unicode_key", "你好世界 🌍 Hello World");
        assertEquals("你好世界 🌍 Hello World", store.get("unicode_key"));
        
        String arabicValue = "مرحبا بالعالم";
        store.set("arabic_key", arabicValue);
        assertEquals(arabicValue, store.get("arabic_key"));
    }

    @Test
    void testSpecialCharacters() {
        KVStore store = KVStore.getInstance();
        
        store.set("key|with|pipes", "value");
        assertEquals("value", store.get("key|with|pipes"));
        
        store.set("key\nwith\nnewlines", "value");
        assertEquals("value", store.get("key\nwith\nnewlines"));
        
        store.set("key\twith\ttabs", "value");
        assertEquals("value", store.get("key\twith\ttabs"));
        
        store.set("key with spaces", "value");
        assertEquals("value", store.get("key with spaces"));
    }

    @Test
    void testJsonSpecialCharacters() {
        KVStore store = KVStore.getInstance();
        
        String jsonValue = "{\"name\": \"test\", \"value\": 123}";
        store.set("json_key", jsonValue);
        assertEquals(jsonValue, store.get("json_key"));
        
        String escapedJson = "{\"path\": \"C:\\\\Users\\\\test\"}";
        store.set("escaped_key", escapedJson);
        assertEquals(escapedJson, store.get("escaped_key"));
    }

    @Test
    void testControlCharacters() {
        KVStore store = KVStore.getInstance();
        
        String controlChars = "value\u0001\u0002\u0003";
        store.set("control_key", controlChars);
        assertEquals(controlChars, store.get("control_key"));
    }

    @Test
    void testUpdateNonExistentKey() {
        KVStore store = KVStore.getInstance();
        
        store.update("nonexistent_key", "new_value");
        assertNull(store.get("nonexistent_key"));
    }

    @Test
    void testDeleteNonExistentKey() {
        KVStore store = KVStore.getInstance();
        
        assertDoesNotThrow(() -> store.delete("nonexistent_key"));
    }

    @Test
    void testGetNonExistentKey() {
        KVStore store = KVStore.getInstance();
        
        assertNull(store.get("nonexistent_key"));
    }

    @Test
    void testMemoryLimitWithSmallConfig() {
        MemoryConfig memoryConfig = new MemoryConfig();
        memoryConfig.setEvictionStrategy(EvictionStrategyType.LRU);
        memoryConfig.setMaxSizeMB(1);
        memoryConfig.setEvictionRatio(0.8);
        
        KVStore store = KVStore.getInstance(new WalConfig(), new ExpiryConfig(), memoryConfig);
        
        for (int i = 0; i < 1000; i++) {
            store.set("small_mem_" + i, "x".repeat(500));
        }
        
        var stats = store.getMemoryStats();
        assertTrue(stats.getUsageRatio() <= 0.95,
            "Memory should be managed within limit: " + stats.getUsageRatio());
    }

    @Test
    void testRapidSetDelete() {
        KVStore store = KVStore.getInstance();
        
        for (int i = 0; i < 1000; i++) {
            String key = "rapid_" + i;
            store.set(key, "value_" + i);
            store.delete(key);
        }
        
        for (int i = 0; i < 1000; i++) {
            assertNull(store.get("rapid_" + i));
        }
    }

    @Test
    void testRapidSetUpdate() {
        KVStore store = KVStore.getInstance();
        
        store.set("rapid_update", "initial");
        
        for (int i = 0; i < 100; i++) {
            store.update("rapid_update", "value_" + i);
        }
        
        assertEquals("value_99", store.get("rapid_update"));
    }

    @Test
    void testSameKeyMultipleOperations() {
        KVStore store = KVStore.getInstance();
        
        store.set("same_key", "value1");
        assertEquals("value1", store.get("same_key"));
        
        store.set("same_key", "value2");
        assertEquals("value2", store.get("same_key"));
        
        store.setex("same_key", "value3", 3600);
        assertEquals("value3", store.get("same_key"));
        
        store.update("same_key", "value4");
        assertEquals("value4", store.get("same_key"));
        
        store.delete("same_key");
        assertNull(store.get("same_key"));
        
        store.set("same_key", "value5");
        assertEquals("value5", store.get("same_key"));
    }

    @Test
    void testMaxTTL() {
        KVStore store = KVStore.getInstance();
        
        store.setex("max_ttl_key", "value", Long.MAX_VALUE / 1000);
        
        assertEquals("value", store.get("max_ttl_key"));
    }

    @Test
    void testVeryLongKeyAndValue() {
        KVStore store = KVStore.getInstance();
        
        String longKey = "k".repeat(100000);
        String longValue = "v".repeat(100000);
        
        store.set(longKey, longValue);
        assertEquals(longValue, store.get(longKey));
    }

    @Test
    void testConcurrentBoundaryAccess() throws InterruptedException {
        KVStore store = KVStore.getInstance();
        
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    String key = "concurrent_boundary_" + threadId + "_" + j;
                    store.set(key, "value_" + j);
                    store.get(key);
                    store.delete(key);
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        assertDoesNotThrow(() -> store.get("any_key"));
    }

    @Test
    void testRecoveryAfterBoundaryOperations() {
        KVStore store = KVStore.getInstance();
        
        store.set("", "empty_key");
        store.set("中文", "中文值");
        store.set("large", "x".repeat(10000));
        store.set("json", "{\"test\": \"value\"}");
        
        store.close();
        KVStore.resetInstance();
        
        KVStore recovered = KVStore.getInstance();
        
        assertEquals("empty_key", recovered.get(""));
        assertEquals("中文值", recovered.get("中文"));
        assertEquals("x".repeat(10000), recovered.get("large"));
        assertEquals("{\"test\": \"value\"}", recovered.get("json"));
    }
}
