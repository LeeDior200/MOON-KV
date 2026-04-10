package com.saki.benchmark.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BenchmarkScenario {
    private String name;
    private String description;
    private WalConfig walConfig;
    private ExpiryConfig expiryConfig;
    private MemoryConfig memoryConfig;
    private BenchmarkParams benchmarkParams;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WalConfig getWalConfig() {
        return walConfig;
    }

    public void setWalConfig(WalConfig walConfig) {
        this.walConfig = walConfig;
    }

    public ExpiryConfig getExpiryConfig() {
        return expiryConfig;
    }

    public void setExpiryConfig(ExpiryConfig expiryConfig) {
        this.expiryConfig = expiryConfig;
    }

    public MemoryConfig getMemoryConfig() {
        return memoryConfig;
    }

    public void setMemoryConfig(MemoryConfig memoryConfig) {
        this.memoryConfig = memoryConfig;
    }

    public BenchmarkParams getBenchmarkParams() {
        return benchmarkParams;
    }

    public void setBenchmarkParams(BenchmarkParams benchmarkParams) {
        this.benchmarkParams = benchmarkParams;
    }

    public static class WalConfig {
        @JsonProperty("flushStrategy")
        private String flushStrategyType = "SYNC";

        @JsonProperty("flushIntervalMs")
        private long flushIntervalMs = 1000;

        @JsonProperty("batchSize")
        private int batchSize = 1000;

        public String getFlushStrategyType() {
            return flushStrategyType;
        }

        public void setFlushStrategyType(String flushStrategyType) {
            this.flushStrategyType = flushStrategyType;
        }

        public long getFlushIntervalMs() {
            return flushIntervalMs;
        }

        public void setFlushIntervalMs(long flushIntervalMs) {
            this.flushIntervalMs = flushIntervalMs;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }
    }

    public static class ExpiryConfig {
        @JsonProperty("strategy")
        private String strategyType = "HYBRID";

        @JsonProperty("scanIntervalMs")
        private long scanIntervalMs = 1000;

        @JsonProperty("scanBatchSize")
        private int scanBatchSize = 100;

        public String getStrategyType() {
            return strategyType;
        }

        public void setStrategyType(String strategyType) {
            this.strategyType = strategyType;
        }

        public long getScanIntervalMs() {
            return scanIntervalMs;
        }

        public void setScanIntervalMs(long scanIntervalMs) {
            this.scanIntervalMs = scanIntervalMs;
        }

        public int getScanBatchSize() {
            return scanBatchSize;
        }

        public void setScanBatchSize(int scanBatchSize) {
            this.scanBatchSize = scanBatchSize;
        }
    }

    public static class MemoryConfig {
        @JsonProperty("evictionStrategy")
        private String evictionStrategy = "LRU";

        @JsonProperty("maxSizeMB")
        private int maxSizeMB = 100;

        @JsonProperty("evictionRatio")
        private double evictionRatio = 0.8;

        public String getEvictionStrategy() {
            return evictionStrategy;
        }

        public void setEvictionStrategy(String evictionStrategy) {
            this.evictionStrategy = evictionStrategy;
        }

        public int getMaxSizeMB() {
            return maxSizeMB;
        }

        public void setMaxSizeMB(int maxSizeMB) {
            this.maxSizeMB = maxSizeMB;
        }

        public double getEvictionRatio() {
            return evictionRatio;
        }

        public void setEvictionRatio(double evictionRatio) {
            this.evictionRatio = evictionRatio;
        }
    }

    public static class BenchmarkParams {
        @JsonProperty("warmupIterations")
        private int warmupIterations = 3;

        @JsonProperty("warmupTime")
        private int warmupTime = 1;

        @JsonProperty("measurementIterations")
        private int measurementIterations = 5;

        @JsonProperty("measurementTime")
        private int measurementTime = 3;

        @JsonProperty("forks")
        private int forks = 1;

        @JsonProperty("threads")
        private int threads = 1;

        public int getWarmupIterations() {
            return warmupIterations;
        }

        public void setWarmupIterations(int warmupIterations) {
            this.warmupIterations = warmupIterations;
        }

        public int getWarmupTime() {
            return warmupTime;
        }

        public void setWarmupTime(int warmupTime) {
            this.warmupTime = warmupTime;
        }

        public int getMeasurementIterations() {
            return measurementIterations;
        }

        public void setMeasurementIterations(int measurementIterations) {
            this.measurementIterations = measurementIterations;
        }

        public int getMeasurementTime() {
            return measurementTime;
        }

        public void setMeasurementTime(int measurementTime) {
            this.measurementTime = measurementTime;
        }

        public int getForks() {
            return forks;
        }

        public void setForks(int forks) {
            this.forks = forks;
        }

        public int getThreads() {
            return threads;
        }

        public void setThreads(int threads) {
            this.threads = threads;
        }
    }
}
