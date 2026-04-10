package com.saki.engine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class KVStoreTest {

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
    void testSetAndGet() {
        KVStore.set("key1", "value1");
        assertEquals("value1", KVStore.get("key1"));
    }

    @Test
    void testSetWithNullKey() {
        KVStore.set(null, "value");
        assertNull(KVStore.get(null));
    }

    @Test
    void testSetWithNullValue() {
        KVStore.set("key", null);
        assertNull(KVStore.get("key"));
    }

    @Test
    void testDelete() {
        KVStore.set("key1", "value1");
        KVStore.delete("key1");
        assertNull(KVStore.get("key1"));
    }

    @Test
    void testUpdate() {
        KVStore.set("key1", "value1");
        KVStore.update("key1", "value2");
        assertEquals("value2", KVStore.get("key1"));
    }

    @Test
    void testUpdateNonExistentKey() {
        KVStore.update("nonexistent", "value");
        assertNull(KVStore.get("nonexistent"));
    }

    @Test
    void testSetex() throws InterruptedException {
        KVStore.setex("key1", "value1", 1);
        assertEquals("value1", KVStore.get("key1"));
        
        Thread.sleep(1100);
        assertNull(KVStore.get("key1"));
    }

    @Test
    void testWalRecovery() {
        KVStore.set("key1", "value1");
        KVStore.set("key2", "value2");
        KVStore.update("key1", "updated_value1");
        KVStore.delete("key2");
        
        KVStore.getInstance().close();
        KVStore.resetInstance();
        
        assertEquals("updated_value1", KVStore.get("key1"));
        assertNull(KVStore.get("key2"));
    }

    @Test
    void testCleanExpiredKeys() {
        KVStore.setex("key1", "value1", 1);
        KVStore.setex("key2", "value2", 2);
        
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            fail("Test interrupted");
        }
        
        KVStore.getInstance().cleanExpiredKeys();
        
        assertNull(KVStore.get("key1"));
        assertEquals("value2", KVStore.get("key2"));
    }
}
