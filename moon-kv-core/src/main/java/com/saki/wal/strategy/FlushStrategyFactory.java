package com.saki.wal.strategy;

import com.saki.wal.config.WalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.RandomAccessFile;

public class FlushStrategyFactory {
    private static final Logger logger = LoggerFactory.getLogger(FlushStrategyFactory.class);

    public static FlushStrategy createStrategy(WalConfig config, RandomAccessFile file) {
        logger.info("Creating flush strategy: {}", config.getFlushStrategyType());
        
        switch (config.getFlushStrategyType()) {
            case SYNC:
                return new SyncFlushStrategy(file);
            case ASYNC:
                return new AsyncFlushStrategy(file, config.getFlushIntervalMs());
            case BATCH:
                return new BatchFlushStrategy(file, config.getBatchSize());
            case CRON:
                return new CronFlushStrategy(file, config.getCronExpression());
            default:
                logger.warn("Unknown flush strategy type: {}, using SYNC", config.getFlushStrategyType());
                return new SyncFlushStrategy(file);
        }
    }
}
