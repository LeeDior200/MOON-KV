package com.saki.wal.strategy;

public interface FlushStrategy {
    void onWrite();
    void flush();
    void shutdown();
}
