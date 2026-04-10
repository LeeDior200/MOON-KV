package com.saki.wal.config;

import com.saki.wal.strategy.FlushStrategyType;

public class WalConfig {
    private FlushStrategyType flushStrategyType = FlushStrategyType.SYNC;
    private long flushIntervalMs = 1000;
    private int batchSize = 1000;
    private String cronExpression = "0 */5 * * * ?";

    public WalConfig() {
    }

    public FlushStrategyType getFlushStrategyType() {
        return flushStrategyType;
    }

    public void setFlushStrategyType(FlushStrategyType flushStrategyType) {
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

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
}
