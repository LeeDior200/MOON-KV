package com.saki.benchmark;

import com.saki.benchmark.config.BenchmarkScenario;
import com.saki.benchmark.config.BenchmarkConfigLoader;
import com.saki.engine.KVStore;
import com.saki.engine.expiry.ExpiryConfig;
import com.saki.engine.expiry.ExpiryStrategyType;
import com.saki.engine.memory.EvictionStrategyType;
import com.saki.engine.memory.MemoryConfig;
import com.saki.wal.config.WalConfig;
import com.saki.wal.strategy.FlushStrategyType;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(3)
public class ConfigurableKVStoreBenchmark {

    private KVStore store;
    private BenchmarkScenario scenario;

    @Param({"sync-lazy-lru", "async-periodic-lfu", "batch-hybrid-lru", "batch-lazy-fifo", "async-hybrid-lru"})
    private String scenarioName;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        List<BenchmarkScenario> scenarios = BenchmarkConfigLoader.loadDefaultScenarios();

        scenario = scenarios.stream()
                .filter(s -> s.getName().equals(scenarioName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + scenarioName));

        System.setProperty("kv.wal.path", "./data/benchmark_" + scenarioName + "_" + System.currentTimeMillis() + ".wal");

        WalConfig walConfig = createWalConfig(scenario);
        ExpiryConfig expiryConfig = createExpiryConfig(scenario);
        MemoryConfig memoryConfig = createMemoryConfig(scenario);

        store = KVStore.getInstance(walConfig, expiryConfig, memoryConfig);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (store != null) {
            store.close();
        }
    }

    @Benchmark
    public void testSet(Blackhole bh) {
        String key = "key-" + Thread.currentThread().getId() + "-" + System.nanoTime();
        String value = "value-" + System.nanoTime();
        KVStore.set(key, value);
        bh.consume(key);
    }

    @Benchmark
    public void testGet(Blackhole bh) {
        String key = "key-" + Thread.currentThread().getId();
        String value = KVStore.get(key);
        bh.consume(value);
    }

    @Benchmark
    public void testSetex(Blackhole bh) {
        String key = "key-" + Thread.currentThread().getId() + "-" + System.nanoTime();
        String value = "value-" + System.nanoTime();
        KVStore.setex(key, value, 3600);
        bh.consume(key);
    }

    @Benchmark
    public void testDelete(Blackhole bh) {
        String key = "key-" + Thread.currentThread().getId() + "-" + System.nanoTime();
        KVStore.delete(key);
        bh.consume(key);
    }

    @Benchmark
    public void testMixedReadWrite(Blackhole bh) {
        long random = System.nanoTime() % 10;
        String key = "key-" + Thread.currentThread().getId();

        if (random < 8) {
            String value = KVStore.get(key);
            bh.consume(value);
        } else {
            String value = "value-" + System.nanoTime();
            KVStore.set(key, value);
            bh.consume(key);
        }
    }

    private WalConfig createWalConfig(BenchmarkScenario scenario) {
        WalConfig config = new WalConfig();

        if (scenario.getWalConfig() != null) {
            BenchmarkScenario.WalConfig walConfig = scenario.getWalConfig();

            String strategyType = walConfig.getFlushStrategyType();
            if ("SYNC".equals(strategyType)) {
                config.setFlushStrategyType(FlushStrategyType.SYNC);
            } else if ("ASYNC".equals(strategyType)) {
                config.setFlushStrategyType(FlushStrategyType.ASYNC);
            } else if ("BATCH".equals(strategyType)) {
                config.setFlushStrategyType(FlushStrategyType.BATCH);
            }

            config.setFlushIntervalMs(walConfig.getFlushIntervalMs());
            config.setBatchSize(walConfig.getBatchSize());
        }

        return config;
    }

    private ExpiryConfig createExpiryConfig(BenchmarkScenario scenario) {
        ExpiryConfig config = new ExpiryConfig();

        if (scenario.getExpiryConfig() != null) {
            BenchmarkScenario.ExpiryConfig expiryConfig = scenario.getExpiryConfig();

            String strategyType = expiryConfig.getStrategyType();
            if ("LAZY".equals(strategyType)) {
                config.setStrategyType(ExpiryStrategyType.LAZY);
            } else if ("PERIODIC".equals(strategyType)) {
                config.setStrategyType(ExpiryStrategyType.PERIODIC);
            } else if ("HYBRID".equals(strategyType)) {
                config.setStrategyType(ExpiryStrategyType.HYBRID);
            }

            config.setScanIntervalMs(expiryConfig.getScanIntervalMs());
            config.setScanBatchSize(expiryConfig.getScanBatchSize());
        }

        return config;
    }

    private MemoryConfig createMemoryConfig(BenchmarkScenario scenario) {
        MemoryConfig config = new MemoryConfig();

        if (scenario.getMemoryConfig() != null) {
            BenchmarkScenario.MemoryConfig memoryConfig = scenario.getMemoryConfig();

            String strategyType = memoryConfig.getEvictionStrategy();
            if ("LRU".equals(strategyType)) {
                config.setEvictionStrategy(EvictionStrategyType.LRU);
            } else if ("LFU".equals(strategyType)) {
                config.setEvictionStrategy(EvictionStrategyType.LFU);
            } else if ("FIFO".equals(strategyType)) {
                config.setEvictionStrategy(EvictionStrategyType.FIFO);
            }

            config.setMaxSizeMB(memoryConfig.getMaxSizeMB());
            config.setEvictionRatio(memoryConfig.getEvictionRatio());
        }

        return config;
    }
}
