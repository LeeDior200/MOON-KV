package com.saki.engine;

import com.saki.engine.expiry.ExpiryConfig;
import com.saki.engine.memory.MemoryConfig;
import com.saki.wal.config.WalConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class StabilityTest {

    @TempDir
    Path tempDir;

    private String testWalPath;

    @BeforeEach
    void setUp() {
        testWalPath = tempDir.resolve("test_stability.wal").toString();
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
    void testMemoryLeakDetection() {
        KVStore store = KVStore.getInstance();
        
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        long initialMemory = memoryBean.getHeapMemoryUsage().getUsed();
        
        for (int cycle = 0; cycle < 10; cycle++) {
            for (int i = 0; i < 10000; i++) {
                store.set("leak_test_" + cycle + "_" + i, "value_" + i);
            }
            
            for (int i = 0; i < 10000; i++) {
                store.delete("leak_test_" + cycle + "_" + i);
            }
        }
        
        System.gc();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        
        long finalMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long memoryIncrease = finalMemory - initialMemory;
        
        System.out.println("Memory leak detection:");
        System.out.println("  Initial memory: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("  Final memory: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("  Memory increase: " + (memoryIncrease / 1024 / 1024) + " MB");
        
        assertTrue(memoryIncrease < 50 * 1024 * 1024,
            "Memory increase should be less than 50MB after cleanup");
    }

    @Test
    void testLongRunningStability_5Minutes() throws InterruptedException {
        KVStore store = KVStore.getInstance();
        
        int threadCount = 10;
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicLong totalOperations = new AtomicLong(0);
        AtomicLong errorCount = new AtomicLong(0);
        
        Thread[] threads = new Thread[threadCount];
        long startTime = System.currentTimeMillis();
        long duration = 5 * 60 * 1000;
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                while (running.get()) {
                    try {
                        String key = "long_run_" + threadId + "_" + System.nanoTime();
                        store.set(key, "value_" + System.nanoTime());
                        store.get(key);
                        store.delete(key);
                        totalOperations.addAndGet(3);
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    }
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        Thread.sleep(duration);
        running.set(false);
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Long running stability test (5 minutes):");
        System.out.println("  Total operations: " + totalOperations.get());
        System.out.println("  Errors: " + errorCount.get());
        System.out.println("  Throughput: " + (totalOperations.get() * 1000.0 / totalTime) + " ops/s");
        
        assertTrue(errorCount.get() < totalOperations.get() * 0.0001,
            "Error rate should be less than 0.01%");
    }

    @Test
    void testPerformanceConsistency() {
        KVStore store = KVStore.getInstance();
        
        int iterations = 10;
        int operationsPerIteration = 10000;
        long[] times = new long[iterations];
        
        for (int iter = 0; iter < iterations; iter++) {
            long start = System.currentTimeMillis();
            
            for (int i = 0; i < operationsPerIteration; i++) {
                store.set("perf_consistency_" + iter + "_" + i, "value_" + i);
                store.get("perf_consistency_" + iter + "_" + i);
            }
            
            times[iter] = System.currentTimeMillis() - start;
        }
        
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        long totalTime = 0;
        
        for (long time : times) {
            minTime = Math.min(minTime, time);
            maxTime = Math.max(maxTime, time);
            totalTime += time;
        }
        
        double avgTime = totalTime / (double) iterations;
        double variance = 0;
        for (long time : times) {
            variance += Math.pow(time - avgTime, 2);
        }
        variance /= iterations;
        double stdDev = Math.sqrt(variance);
        
        System.out.println("Performance consistency test:");
        System.out.println("  Iterations: " + iterations);
        System.out.println("  Operations per iteration: " + operationsPerIteration);
        System.out.println("  Min time: " + minTime + "ms");
        System.out.println("  Max time: " + maxTime + "ms");
        System.out.println("  Avg time: " + String.format("%.2f", avgTime) + "ms");
        System.out.println("  Std deviation: " + String.format("%.2f", stdDev) + "ms");
        
        assertTrue(maxTime < minTime * 3,
            "Performance should be consistent (max time should not be 3x min time)");
    }

    @Test
    void testGcPressure() {
        KVStore store = KVStore.getInstance();
        
        long gcCountBefore = 0;
        long gcTimeBefore = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCountBefore += gcBean.getCollectionCount();
            gcTimeBefore += gcBean.getCollectionTime();
        }
        
        for (int i = 0; i < 100000; i++) {
            String key = "gc_pressure_" + i;
            String value = "x".repeat(100);
            store.set(key, value);
            if (i % 1000 == 0) {
                store.delete("gc_pressure_" + (i - 500));
            }
        }
        
        long gcCountAfter = 0;
        long gcTimeAfter = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCountAfter += gcBean.getCollectionCount();
            gcTimeAfter += gcBean.getCollectionTime();
        }
        
        long gcCount = gcCountAfter - gcCountBefore;
        long gcTime = gcTimeAfter - gcTimeBefore;
        
        System.out.println("GC pressure test:");
        System.out.println("  GC count: " + gcCount);
        System.out.println("  GC time: " + gcTime + "ms");
        
        assertTrue(gcTime < 30000, "GC time should be reasonable (< 30s)");
    }

    @Test
    void testMemoryUsageTrend() {
        KVStore store = KVStore.getInstance();
        
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long[] memorySamples = new long[10];
        
        for (int sample = 0; sample < 10; sample++) {
            for (int i = 0; i < 10000; i++) {
                store.set("mem_trend_" + sample + "_" + i, "value_" + i);
            }
            
            memorySamples[sample] = memoryBean.getHeapMemoryUsage().getUsed();
        }
        
        boolean memoryGrowing = false;
        for (int i = 1; i < memorySamples.length; i++) {
            if (memorySamples[i] > memorySamples[i - 1] * 1.5) {
                memoryGrowing = true;
                break;
            }
        }
        
        System.out.println("Memory usage trend test:");
        for (int i = 0; i < memorySamples.length; i++) {
            System.out.println("  Sample " + i + ": " + (memorySamples[i] / 1024 / 1024) + " MB");
        }
        
        var stats = store.getMemoryStats();
        System.out.println("  Store entries: " + stats.getEntryCount());
        System.out.println("  Store memory: " + (stats.getUsedBytes() / 1024 / 1024) + " MB");
    }

    @Test
    void testRepeatedOpenClose() {
        int iterations = 10;
        
        for (int i = 0; i < iterations; i++) {
            KVStore store = KVStore.getInstance();
            
            store.set("open_close_" + i, "value_" + i);
            assertEquals("value_" + i, store.get("open_close_" + i));
            
            store.close();
            KVStore.resetInstance();
        }
        
        KVStore finalStore = KVStore.getInstance();
        for (int i = 0; i < iterations; i++) {
            assertEquals("value_" + i, finalStore.get("open_close_" + i),
                "Data should persist after repeated open/close");
        }
    }

    @Test
    void testResourceCleanup() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long initialMemory = memoryBean.getHeapMemoryUsage().getUsed();
        
        for (int cycle = 0; cycle < 5; cycle++) {
            KVStore store = KVStore.getInstance();
            
            for (int i = 0; i < 10000; i++) {
                store.set("resource_" + cycle + "_" + i, "x".repeat(100));
            }
            
            store.close();
            KVStore.resetInstance();
            
            System.gc();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }
        
        long finalMemory = memoryBean.getHeapMemoryUsage().getUsed();
        
        System.out.println("Resource cleanup test:");
        System.out.println("  Initial memory: " + (initialMemory / 1024 / 1024) + " MB");
        System.out.println("  Final memory: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("  Difference: " + ((finalMemory - initialMemory) / 1024 / 1024) + " MB");
    }

    @Test
    void testConcurrentStability() throws InterruptedException {
        KVStore store = KVStore.getInstance();
        
        int threadCount = 50;
        int duration = 30000;
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicLong operations = new AtomicLong(0);
        AtomicLong errors = new AtomicLong(0);
        
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                while (running.get()) {
                    try {
                        String key = "concurrent_stable_" + Thread.currentThread().getId();
                        store.set(key, "value_" + System.nanoTime());
                        store.get(key);
                        store.delete(key);
                        operations.incrementAndGet();
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    }
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        Thread.sleep(duration);
        running.set(false);
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        System.out.println("Concurrent stability test:");
        System.out.println("  Duration: " + duration + "ms");
        System.out.println("  Threads: " + threadCount);
        System.out.println("  Operations: " + operations.get());
        System.out.println("  Errors: " + errors.get());
        System.out.println("  Error rate: " + String.format("%.4f%%", 
            errors.get() * 100.0 / operations.get()));
        
        assertTrue(errors.get() < operations.get() * 0.001,
            "Error rate should be less than 0.1%");
    }

    @Test
    void testWalFileSizeGrowth() {
        KVStore store = KVStore.getInstance();
        
        java.io.File walFile = new java.io.File(testWalPath);
        long initialSize = walFile.exists() ? walFile.length() : 0;
        
        for (int i = 0; i < 10000; i++) {
            store.set("wal_growth_" + i, "value_" + i);
            store.delete("wal_growth_" + i);
        }
        
        long finalSize = walFile.length();
        
        System.out.println("WAL file size growth test:");
        System.out.println("  Initial size: " + (initialSize / 1024) + " KB");
        System.out.println("  Final size: " + (finalSize / 1024) + " KB");
        System.out.println("  Growth: " + ((finalSize - initialSize) / 1024) + " KB");
        
        assertTrue(finalSize < 100 * 1024 * 1024,
            "WAL file should not grow excessively (< 100MB)");
    }
}
