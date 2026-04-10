package com.saki.engine.expiry;

public interface ExpiryStrategy {
    void start();
    void stop();
    void onAccess(String key);
}
