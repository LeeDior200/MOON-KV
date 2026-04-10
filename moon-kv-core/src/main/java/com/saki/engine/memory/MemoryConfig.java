package com.saki.engine.memory;

public class MemoryConfig {
    private long maxSizeBytes = 256 * 1024 * 1024; // 256 MB default
    private EvictionStrategyType evictionStrategy = EvictionStrategyType.LRU;
    private double evictionRatio = 0.8; // Start evicting when 80% full

    public MemoryConfig() {
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public void setMaxSizeMB(int maxSizeMB) {
        this.maxSizeBytes = maxSizeMB * 1024L * 1024L;
    }

    public EvictionStrategyType getEvictionStrategy() {
        return evictionStrategy;
    }

    public void setEvictionStrategy(EvictionStrategyType evictionStrategy) {
        this.evictionStrategy = evictionStrategy;
    }

    public double getEvictionRatio() {
        return evictionRatio;
    }

    public void setEvictionRatio(double evictionRatio) {
        this.evictionRatio = evictionRatio;
    }
}
