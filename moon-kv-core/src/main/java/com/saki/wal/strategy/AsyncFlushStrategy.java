package com.saki.wal.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncFlushStrategy implements FlushStrategy {
    private static final Logger logger = LoggerFactory.getLogger(AsyncFlushStrategy.class);
    
    private final RandomAccessFile file;
    private final long flushIntervalMs;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean needsFlush = new AtomicBoolean(false);
    private volatile boolean running = true;

    public AsyncFlushStrategy(RandomAccessFile file, long flushIntervalMs) {
        this.file = file;
        this.flushIntervalMs = flushIntervalMs;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WAL-AsyncFlush");
            t.setDaemon(true);
            return t;
        });
        
        startScheduler();
        logger.info("AsyncFlushStrategy initialized with interval {} ms", flushIntervalMs);
    }

    private void startScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            if (needsFlush.get() && running) {
                flush();
            }
        }, flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onWrite() {
        needsFlush.set(true);
    }

    @Override
    public void flush() {
        if (!running) {
            return;
        }
        
        try {
            file.getFD().sync();
            needsFlush.set(false);
            logger.debug("Async flush completed");
        } catch (IOException e) {
            logger.error("Failed to async flush", e);
            throw new RuntimeException("Failed to async flush", e);
        }
    }

    @Override
    public void shutdown() {
        running = false;
        if (needsFlush.get()) {
            flush();
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("AsyncFlushStrategy shutdown");
    }
}
