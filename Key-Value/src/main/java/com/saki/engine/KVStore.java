package com.saki.engine;
import com.saki.wal.Wal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KVStore implements AutoCloseable{
    private static volatile KVStore instance;
    private final ConcurrentHashMap<String, String> store;
    private final ConcurrentHashMap<String, Long> expireAt;
    private final Wal wal;
    // 私有构造
    private KVStore(String walPath) {
        this.store = new ConcurrentHashMap<>();
        this.expireAt =new ConcurrentHashMap<>();
        this.wal = new Wal(walPath);
        loadFromWal();
    }

    // 全局访问点
    public static KVStore getInstance() {
        if (instance == null) {
            synchronized (KVStore.class) {
                if (instance == null) {
                    // 第一次使用时才创建，路径可以配置
                    String walPath = System.getProperty("kv.wal.path", "./data/kv_store.wal");
                    instance = new KVStore(walPath);
                    // 注册关闭钩子
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
    // 用户直接调静态方法
    public static void set(String key, String value) {
        getInstance().doSet(key, value);
    }

    public static void setex(String key,String value,long ttlSeconds){
        getInstance().doSetex(key,value,ttlSeconds);
    }


    public static String get(String key) {
        return getInstance().doGet(key);
    }

    public static void delete(String key) {
        getInstance().doDelete(key);
    }

    public static void update(String key,String newValue){
        getInstance().doUpdate(key,newValue);
    }


    private void doSetex(String key, String value, long ttlSeconds) {
        if (key == null || value == null) {
            return;
        }

        long expireTime = System.currentTimeMillis() + ttlSeconds * 1000;

        // 写WAL（需要记录过期时间）
        wal.write("SETEX", key, value + "|" + expireTime);

        // 写内存
        store.put(key, value);
        expireAt.put(key, expireTime);

    }

    // 实例方法
    private void doSet(String key, String value) {
        if (key == null || value == null) {
            return;
        }
        wal.write("SET", key, value);
        store.put(key, value);
    }

    private String doGet(String key) {
        if (key == null) {
            return null;
        }
        Long expire = expireAt.get(key);
        if (expire != null && System.currentTimeMillis() > expire) {
            // 过期了，删除
            delete(key);
            return null;
        }
        return store.get(key);
    }

    private void doDelete(String key) {
        if (key == null) {
            return;
        }
        wal.write("DEL", key, null);
        store.remove(key);
        expireAt.remove(key);
    }

    private void doUpdate(String key, String newValue) {
        if (key == null || newValue == null) {
            return;
        }
        wal.write("UPD",key,newValue);
        store.remove(key,newValue);
    }
    private void loadFromWal() {
        wal.replay((op, key, value) -> {
            switch (op){
                case "SET":
                    store.put(key,value);
                    break;
                case "DEL":
                    store.remove(key);
                    break;
                case "UPD":
                    store.replace(key,value);
                    break;
                case "SETEX":
                    String[] parts = value.split("\\|");
                    if (parts.length == 2) {
                        store.put(key, parts[0]);
                        expireAt.put(key, Long.parseLong(parts[1]));
                    }
                    break;
                default:
                    break;
            }

        });
    }
    public void cleanExpiredKeys() {
        long now = System.currentTimeMillis();
        // 遍历过期时间Map，删除过期的key
        for (Map.Entry<String, Long> entry : expireAt.entrySet()) {
            if (now > entry.getValue()) {
                String key = entry.getKey();
                store.remove(key);
                expireAt.remove(key);
                // 注意：这里不写WAL，因为过期删除不需要持久化
            }
        }
    }

    @Override
    public void close()  {
        wal.close();
    }
}
