package com.saki.benchmark;

import com.saki.engine.KVStore;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(3)
public class KVStoreBenchmark {

    private KVStore store;

    @Setup
    public void setup() {
        System.setProperty("kv.wal.path", "./data/benchmark_" + System.currentTimeMillis() + ".wal");
        store = KVStore.getInstance();
    }

    @TearDown
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
}
