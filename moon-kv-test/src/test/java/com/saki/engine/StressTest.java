package com.saki.engine;

import com.saki.engine.expiry.ExpiryConfig;
import com.saki.engine.memory.MemoryConfig;
import com.saki.wal.config.WalConfig;
import com.saki.wal.strategy.FlushStrategyType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class StressTest {

    @TempDir
    Path tempDir;

    private String testWalPath;

    @BeforeEach
    void setUp() {
        testWalPath = tempDir.resolve("test_stress.wal").toString();
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
    void testLargeDataVolume_10K() {
        KVStore store = KVStore.getInstance();
        
        int dataSize = 10000;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < dataSize; i++) {
            store.set("volume_10k_" + i, "value_" + i);
        }
        
        long writeTime = System.currentTimeMillis() - startTime;
        System.out.println("Write " + dataSize + " records: " + writeTime + "ms");
        
        startTime = System.currentTimeMillis();
        for (int i = 0; i < dataSize; i++) {
            assertEquals("value_" + i, store.get("volume_10k_" + i));
        }
        long readTime = System.currentTimeMillis() - startTime;
        System.out.println("Read " + dataSize + " records: " + readTime + "ms");
        
        var stats = store.getMemoryStats();
        assertEquals(dataSize, stats.getEntryCount());
    }

    @Test
    void testLargeDataVolume_100K() {
        KVStore store = KVStore.getInstance();
        
        int dataSize = 100000;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < dataSize; i++) {
            store.set("volume_100k_" + i, "value_" + i);
        }
        
        long writeTime = System.currentTimeMillis() - startTime;
        System.out.println("Write " + dataSize + " records: " + writeTime + "ms (" + 
            (dataSize * 1000.0 / writeTime) + " ops/s)");
        
        startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            int idx = (int) (Math.random() * dataSize);
            store.get("volume_100k_" + idx);
        }
        long readTime = System.currentTimeMillis() - startTime;
        System.out.println("Read 1000 random records from " + dataSize + ": " + readTime + "ms");
        
        var stats = store.getMemoryStats();
        assertEquals(dataSize, stats.getEntryCount());
    }

    @Test
    void testHighConcurrency_100Threads() throws InterruptedException {
        KVStore store = KVStore.getInstance();
        
        int threadCount = 100;
        int operationsPerThread = 100;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        Thread[] threads = new Thread[threadCount];
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "concurrent_100_" + threadId + "_" + j;
                        store.set(key, "value_" + j);
                        String value = store.get(key);
                        if (("value_" + j).equals(value)) {
                            successCount.incrementAndGet();
                        }
                        store.delete(key);
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        int totalOperations = threadCount * operationsPerThread * 3;
        
        System.out.println("100 threads × 100 operations completed in " + totalTime + "ms");
        System.out.println("Total operations: " + totalOperations);
        System.out.println("Throughput: " + (totalOperations * 1000.0 / totalTime) + " ops/s");
        System.out.println("Success: " + successCount.get() + ", Errors: " + errorCount.get());
        
        assertEquals(0, errorCount.get(), "No errors should occur during concurrent access");
    }

    @Test
    void testHighConcurrency_500Threads() throws InterruptedException {
        KVStore store = KVStore.getInstance();
        
        int threadCount = 500;
        int operationsPerThread = 50;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        Thread[] threads = new Thread[threadCount];
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "concurrent_500_" + threadId + "_" + j;
                        store.set(key, "value_" + j);
                        store.get(key);
                        store.delete(key);
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        latch.await();
        
        long totalTime = System.currentTimeMillis() - startTime;
        int totalOperations = threadCount * operationsPerThread * 3;
        
        System.out.println("500 threads × 50 operations completed in " + totalTime + "ms");
        System.out.println("Throughput: " + (totalOperations * 1000.0 / totalTime) + " ops/s");
        System.out.println("Errors: " + errorCount.get());
        
        assertTrue(errorCount.get() < totalOperations * 0.01,
            "Error rate should be less than 1%");
    }

    @Test
    void testMixedWorkload_ReadHeavy() throws InterruptedException {
        KVStore store = KVStore.getInstance();
        
        for (int i = 0; i < 1000; i++) {
            store.set("read_heavy_" + i, "value_" + i);
        }
        
        int threadCount = 50;
        int operationsPerThread = 200;
        AtomicLong readCount = new AtomicLong(0);
        AtomicLong writeCount = new AtomicLong(0);
        
        Thread[] threads = new Thread[threadCount];
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    if (Math.random() < 0.8) {
                        int idx = (int) (Math.random() * 1000);
                        store.get("read_heavy_" + idx);
                        readCount.incrementAndGet();
                    } else {
                        String key = "read_heavy_new_" + Thread.currentThread().getId() + "_" + j;
                        store.set(key, "new_value");
                        writeCount.incrementAndGet();
                    }
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("Read-heavy workload (80% read, 20% write):");
        System.out.println("  Total time: " + totalTime + "ms");
        System.out.println("  Reads: " + readCount.get() + ", Writes: " + writeCount.get());
        System.out.println("  Throughput: " + ((readCount.get() + writeCount.get()) * 1000.0 / totalTime) + " ops/s");
    }

    @Test
    void testMixedWorkload_WriteHeavy() throws InterruptedException {
        KVStore store = KVStore.getInstance();
        
        int threadCount = 50;
        int operationsPerThread = 200;
        AtomicLong readCount = new AtomicLong(0);
        AtomicLong writeCount = new AtomicLong(0);
        
        Thread[] threads = new Thread[threadCount];
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    if (Math.random() < 0.3) {
                        int idx = (int) (Math.random() * 1000);
                        store.get("write_heavy_" + idx);
                        readCount.incrementAndGet();
                    } else {
                        String key = "write_heavy_" + Thread.currentThread().getId() + "_" + j;
                        store.set(key, "value_" + j);
                        writeCount.incrementAndGet();
                    }
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("Write-heavy workload (30% read, 70% write):");
        System.out.println("  Total time: " + totalTime + "ms");
        System.out.println("  Reads: " + readCount.get() + ", Writes: " + writeCount.get());
        System.out.println("  Throughput: " + ((readCount.get() + writeCount.get()) * 1000.0 / totalTime) + " ops/s");
    }

    @Test
    void testMixedWorkload_Balanced() throws InterruptedException {
        KVStore store = KVStore.getInstance();
        
        int threadCount = 50;
        int operationsPerThread = 200;
        AtomicLong readCount = new AtomicLong(0);
        AtomicLong writeCount = new AtomicLong(0);
        
        Thread[] threads = new Thread[threadCount];
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    if (Math.random() < 0.5) {
                        int idx = (int) (Math.random() * 1000);
                        store.get("balanced_" + idx);
                        readCount.incrementAndGet();
                    } else {
                        String key = "balanced_" + Thread.currentThread().getId() + "_" + j;
                        store.set(key, "value_" + j);
                        writeCount.incrementAndGet();
                    }
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("Balanced workload (50% read, 50% write):");
        System.out.println("  Total time: " + totalTime + "ms");
        System.out.println("  Reads: " + readCount.get() + ", Writes: " + writeCount.get());
        System.out.println("  Throughput: " + ((readCount.get() + writeCount.get()) * 1000.0 / totalTime) + " ops/s");
    }

    @Test
    void testSustainedLoad_1Minute() throws InterruptedException {
        KVStore store = KVStore.getInstance();
        
        int threadCount = 20;
        AtomicLong totalOperations = new AtomicLong(0);
        AtomicInteger running = new AtomicInteger(1);
        
        Thread[] threads = new Thread[threadCount];
        long startTime = System.currentTimeMillis();
        long duration = 60 * 1000;
        
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                while (running.get() == 1) {
                    String key = "sustained_" + Thread.currentThread().getId() + "_" + System.nanoTime();
                    store.set(key, "value");
                    store.get(key);
                    store.delete(key);
                    totalOperations.addAndGet(3);
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        Thread.sleep(duration);
        running.set(0);
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("Sustained load test (1 minute):");
        System.out.println("  Total operations: " + totalOperations.get());
        System.out.println("  Throughput: " + (totalOperations.get() * 1000.0 / totalTime) + " ops/s");
        
        assertTrue(totalOperations.get() > 0, "Should complete some operations");
    }

    @Test
    void testBatchFlushStress() {
        WalConfig walConfig = new WalConfig();
        walConfig.setFlushStrategyType(FlushStrategyType.BATCH);
        walConfig.setBatchSize(100);
        
        KVStore store = KVStore.getInstance(walConfig, new ExpiryConfig(), new MemoryConfig());
        
        int dataSize = 50000;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < dataSize; i++) {
            store.set("batch_stress_" + i, "value_" + i);
        }
        
        long writeTime = System.currentTimeMillis() - startTime;
        System.out.println("Batch flush stress test:");
        System.out.println("  Write " + dataSize + " records: " + writeTime + "ms");
        System.out.println("  Throughput: " + (dataSize * 1000.0 / writeTime) + " ops/s");
        
        for (int i = 0; i < 100; i++) {
            int idx = (int) (Math.random() * dataSize);
            assertNotNull(store.get("batch_stress_" + idx));
        }
    }

    @Test
    void testAsyncFlushStress() throws InterruptedException {
        WalConfig walConfig = new WalConfig();
        walConfig.setFlushStrategyType(FlushStrategyType.ASYNC);
        walConfig.setFlushIntervalMs(50);
        
        KVStore store = KVStore.getInstance(walConfig, new ExpiryConfig(), new MemoryConfig());
        
        int dataSize = 50000;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < dataSize; i++) {
            store.set("async_stress_" + i, "value_" + i);
        }
        
        long writeTime = System.currentTimeMillis() - startTime;
        System.out.println("Async flush stress test:");
        System.out.println("  Write " + dataSize + " records: " + writeTime + "ms");
        System.out.println("  Throughput: " + (dataSize * 1000.0 / writeTime) + " ops/s");
        
        Thread.sleep(200);
        
        for (int i = 0; i < 100; i++) {
            int idx = (int) (Math.random() * dataSize);
            assertNotNull(store.get("async_stress_" + idx));
        }
    }

    @Test
    void testMemoryUnderStress() {
        MemoryConfig memoryConfig = new MemoryConfig();
        memoryConfig.setMaxSizeMB(50);
        
        KVStore store = KVStore.getInstance(new WalConfig(), new ExpiryConfig(), memoryConfig);
        
        int dataSize = 100000;
        for (int i = 0; i < dataSize; i++) {
            store.set("memory_stress_" + i, "x".repeat(100));
        }
        
        var stats = store.getMemoryStats();
        System.out.println("Memory under stress:");
        System.out.println("  Entries: " + stats.getEntryCount());
        System.out.println("  Used bytes: " + stats.getUsedBytes());
        System.out.println("  Usage ratio: " + String.format("%.2f%%", stats.getUsageRatio() * 100));
        
        assertTrue(stats.getUsageRatio() <= 1.0, "Memory usage should not exceed limit");
    }
}
