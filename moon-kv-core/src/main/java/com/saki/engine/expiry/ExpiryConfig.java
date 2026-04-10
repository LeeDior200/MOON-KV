package com.saki.engine.expiry;

public class ExpiryConfig {
    private ExpiryStrategyType strategyType = ExpiryStrategyType.HYBRID;
    private long scanIntervalMs = 1000;
    private int scanBatchSize = 100;

    public ExpiryConfig() {
    }

    public ExpiryStrategyType getStrategyType() {
        return strategyType;
    }

    public void setStrategyType(ExpiryStrategyType strategyType) {
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
