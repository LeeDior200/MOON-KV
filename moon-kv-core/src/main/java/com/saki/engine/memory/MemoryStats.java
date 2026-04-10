package com.saki.engine.memory;

public class MemoryStats {
    private final long usedBytes;
    private final long maxBytes;
    private final int entryCount;
    private final double usageRatio;

    public MemoryStats(long usedBytes, long maxBytes, int entryCount) {
        this.usedBytes = usedBytes;
        this.maxBytes = maxBytes;
        this.entryCount = entryCount;
        this.usageRatio = maxBytes > 0 ? (double) usedBytes / maxBytes : 0.0;
    }

    public long getUsedBytes() {
        return usedBytes;
    }

    public long getMaxBytes() {
        return maxBytes;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public double getUsageRatio() {
        return usageRatio;
    }

    @Override
    public String toString() {
        return String.format("MemoryStats{used=%d MB, max=%d MB, entries=%d, usage=%.2f%%}",
            usedBytes / 1024 / 1024, maxBytes / 1024 / 1024, entryCount, usageRatio * 100);
    }
}
