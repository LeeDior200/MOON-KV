package com.saki.wal.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;

public class SyncFlushStrategy implements FlushStrategy {
    private static final Logger logger = LoggerFactory.getLogger(SyncFlushStrategy.class);
    
    private final RandomAccessFile file;

    public SyncFlushStrategy(RandomAccessFile file) {
        this.file = file;
        logger.info("SyncFlushStrategy initialized");
    }

    @Override
    public void onWrite() {
        flush();
    }

    @Override
    public void flush() {
        try {
            file.getFD().sync();
            logger.debug("Sync flush completed");
        } catch (IOException e) {
            logger.error("Failed to sync flush", e);
            throw new RuntimeException("Failed to sync flush", e);
        }
    }

    @Override
    public void shutdown() {
        logger.info("SyncFlushStrategy shutdown");
    }
}
