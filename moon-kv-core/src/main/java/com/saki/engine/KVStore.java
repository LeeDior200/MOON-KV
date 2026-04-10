package com.saki.engine;

import com.saki.engine.expiry.ExpiryConfig;
import com.saki.engine.expiry.ExpiryManager;
import com.saki.engine.memory.*;
import com.saki.wal.Wal;
import com.saki.wal.config.WalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KVStore implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(KVStore.class);
    
    private static volatile KVStore instance;
    private final ConcurrentHashMap<String, String> store;
    private final ConcurrentHashMap<String, Long> expireAt;
    private final Wal wal;
    private final ExpiryManager expiryManager;
    private final MemoryManager memoryManager;
    private final WalConfig walConfig;
    private final ExpiryConfig expiryConfig;
    private final MemoryConfig memoryConfig;

    private KVStore(String walPath) {
        this(walPath, new WalConfig(), new ExpiryConfig(), new MemoryConfig());
    }

    private KVStore(String walPath, WalConfig walConfig, ExpiryConfig expiryConfig, MemoryConfig memoryConfig) {
        this.walConfig = walConfig;
        this.expiryConfig = expiryConfig;
        this.memoryConfig = memoryConfig;
        
        this.store = new ConcurrentHashMap<>();
        this.expireAt = new ConcurrentHashMap<>();
        this.wal = new Wal(walPath, walConfig);
        
        this.memoryManager = createMemoryManager();
        this.expiryManager = new ExpiryManager(expireAt, store, expiryConfig, (key, value) -> {
            logger.debug("Key expired: {}", key);
        });
        
        loadFromWal();
        expiryManager.start();
        
        logger.info("KVStore initialized with WAL strategy: {}, Expiry strategy: {}, Memory strategy: {}",
            walConfig.getFlushStrategyType(), expiryConfig.getStrategyType(), memoryConfig.getEvictionStrategy());
    }

    private MemoryManager createMemoryManager() {
        switch (memoryConfig.getEvictionStrategy()) {
            case LRU:
                return new LRUMemoryManager(store, memoryConfig);
            case LFU:
                return new LFUMemoryManager(store, memoryConfig);
            case FIFO:
                return new FIFOMemoryManager(store, memoryConfig);
            default:
                logger.warn("Unknown eviction strategy: {}, using LRU", memoryConfig.getEvictionStrategy());
                return new LRUMemoryManager(store, memoryConfig);
        }
    }

    public static KVStore getInstance() {
        if (instance == null) {
            synchronized (KVStore.class) {
                if (instance == null) {
                    String walPath = System.getProperty("kv.wal.path", "./data/kv_store.wal");
                    instance = new KVStore(walPath);
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        if (instance != null) {
                            instance.close();
                        }
                    }));
                }
            }
        }
        return instance;
    }

    public static KVStore getInstance(WalConfig walConfig, ExpiryConfig expiryConfig, MemoryConfig memoryConfig) {
        if (instance == null) {
            synchronized (KVStore.class) {
                if (instance == null) {
                    String walPath = System.getProperty("kv.wal.path", "./data/kv_store.wal");
                    instance = new KVStore(walPath, walConfig, expiryConfig, memoryConfig);
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        if (instance != null) {
                            instance.close();
                        }
                    }));
                }
            }
        }
        return instance;
    }

    static void resetInstance() {
        instance = null;
    }

    public static void set(String key, String value) {
        getInstance().doSet(key, value);
    }

    public static void setex(String key, String value, long ttlSeconds) {
        getInstance().doSetex(key, value, ttlSeconds);
    }

    public static String get(String key) {
        return getInstance().doGet(key);
    }

    public static void delete(String key) {
        getInstance().doDelete(key);
    }

    public static void update(String key, String newValue) {
        getInstance().doUpdate(key, newValue);
    }

    private void doSetex(String key, String value, long ttlSeconds) {
        if (key == null || value == null) {
            logger.warn("Attempted to set null key or value");
            return;
        }

        if (!memoryManager.canPut(key, value)) {
            memoryManager.evict();
        }

        long expireTime = System.currentTimeMillis() + ttlSeconds * 1000;

        wal.write("SETEX", key, value + "|" + expireTime);

        String oldValue = store.put(key, value);
        expireAt.put(key, expireTime);
        
        if (oldValue == null) {
            memoryManager.onPut(key, value);
        } else {
            memoryManager.onUpdate(key, oldValue, value);
        }

        logger.debug("Set key with expiry: {} (TTL: {}s)", key, ttlSeconds);
    }

    private void doSet(String key, String value) {
        if (key == null || value == null) {
            logger.warn("Attempted to set null key or value");
            return;
        }

        if (!memoryManager.canPut(key, value)) {
            memoryManager.evict();
        }

        wal.write("SET", key, value);
        
        String oldValue = store.put(key, value);
        
        if (oldValue == null) {
            memoryManager.onPut(key, value);
        } else {
            memoryManager.onUpdate(key, oldValue, value);
        }

        logger.debug("Set key: {}", key);
    }

    private String doGet(String key) {
        if (key == null) {
            return null;
        }

        expiryManager.onAccess(key);
        
        Long expire = expireAt.get(key);
        if (expire != null && System.currentTimeMillis() > expire) {
            delete(key);
            logger.debug("Key expired on access: {}", key);
            return null;
        }
        
        String value = store.get(key);
        logger.debug("Get key: {} -> {}", key, value != null ? "found" : "not found");
        return value;
    }

    private void doDelete(String key) {
        if (key == null) {
            return;
        }
        
        wal.write("DEL", key, null);
        String value = store.remove(key);
        expireAt.remove(key);
        
        if (value != null) {
            memoryManager.onRemove(key, value);
        }

        logger.debug("Deleted key: {}", key);
    }

    private void doUpdate(String key, String newValue) {
        if (key == null || newValue == null) {
            logger.warn("Attempted to update with null key or value");
            return;
        }

        String oldValue = store.get(key);
        if (oldValue == null) {
            logger.warn("Attempted to update non-existent key: {}", key);
            return;
        }

        if (!memoryManager.canPut(key, newValue)) {
            memoryManager.evict();
        }

        wal.write("UPD", key, newValue);
        store.replace(key, newValue);
        memoryManager.onUpdate(key, oldValue, newValue);

        logger.debug("Updated key: {}", key);
    }

    private void loadFromWal() {
        logger.info("Loading data from WAL...");
        wal.replay((op, key, value) -> {
            switch (op) {
                case "SET":
                    store.put(key, value);
                    memoryManager.onPut(key, value);
                    break;
                case "DEL":
                    String oldValue = store.remove(key);
                    if (oldValue != null) {
                        memoryManager.onRemove(key, oldValue);
                    }
                    break;
                case "UPD":
                    String oldVal = store.get(key);
                    store.replace(key, value);
                    if (oldVal != null) {
                        memoryManager.onUpdate(key, oldVal, value);
                    }
                    break;
                case "SETEX":
                    String[] parts = value.split("\\|");
                    if (parts.length == 2) {
                        store.put(key, parts[0]);
                        expireAt.put(key, Long.parseLong(parts[1]));
                        memoryManager.onPut(key, parts[0]);
                    }
                    break;
                default:
                    logger.warn("Unknown operation in WAL: {}", op);
                    break;
            }
        });
        logger.info("WAL replay completed");
    }

    public void cleanExpiredKeys() {
        expiryManager.cleanExpiredKeys();
    }

    public MemoryStats getMemoryStats() {
        return memoryManager.getStats();
    }

    public void evict() {
        memoryManager.evict();
    }

    @Override
    public void close() {
        logger.info("Closing KVStore...");
        expiryManager.stop();
        wal.close();
        logger.info("KVStore closed");
    }
}
