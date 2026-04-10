package com.saki.wal.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchFlushStrategy implements FlushStrategy {
    private static final Logger logger = LoggerFactory.getLogger(BatchFlushStrategy.class);
    
    private final RandomAccessFile file;
    private final int batchSize;
    private final AtomicInteger writeCount = new AtomicInteger(0);

    public BatchFlushStrategy(RandomAccessFile file, int batchSize) {
        this.file = file;
        this.batchSize = batchSize;
        logger.info("BatchFlushStrategy initialized with batch size {}", batchSize);
    }

    @Override
    public void onWrite() {
        int count = writeCount.incrementAndGet();
        if (count >= batchSize) {
            flush();
        }
    }

    @Override
    public void flush() {
        try {
            file.getFD().sync();
            writeCount.set(0);
            logger.debug("Batch flush completed, flushed {} records", batchSize);
        } catch (IOException e) {
            logger.error("Failed to batch flush", e);
            throw new RuntimeException("Failed to batch flush", e);
        }
    }

    @Override
    public void shutdown() {
        if (writeCount.get() > 0) {
            logger.info("Flushing remaining {} records before shutdown", writeCount.get());
            flush();
        }
        logger.info("BatchFlushStrategy shutdown");
    }
}
