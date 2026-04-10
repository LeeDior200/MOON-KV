package com.saki.engine.memory;

public interface MemoryManager {
    boolean canPut(String key, String value);
    void onPut(String key, String value);
    void onRemove(String key, String value);
    void onUpdate(String key, String oldValue, String newValue);
    MemoryStats getStats();
    void evict();
}
