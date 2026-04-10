package com.saki.benchmark;

import com.saki.wal.Wal;
import com.saki.wal.config.WalConfig;
import com.saki.wal.strategy.FlushStrategyType;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(3)
public class WALBenchmark {

    private Wal wal;

    @Setup
    public void setup() {
        WalConfig config = new WalConfig();
        config.setFlushStrategyType(FlushStrategyType.BATCH);
        wal = new Wal("./data/benchmark_wal_" + System.currentTimeMillis() + ".wal", config);
    }

    @TearDown
    public void tearDown() {
        if (wal != null) {
            wal.close();
        }
    }

    @Benchmark
    public void testWrite(Blackhole bh) {
        String key = "key-" + System.nanoTime();
        String value = "value-" + System.nanoTime();
        wal.write("SET", key, value);
        bh.consume(key);
    }
}
