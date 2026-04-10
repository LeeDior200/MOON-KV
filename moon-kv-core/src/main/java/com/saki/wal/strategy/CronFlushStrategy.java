package com.saki.wal.strategy;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CronFlushStrategy implements FlushStrategy {
    private static final Logger logger = LoggerFactory.getLogger(CronFlushStrategy.class);
    
    private final RandomAccessFile file;
    private final String cronExpression;
    private final ScheduledExecutorService scheduler;
    private final Cron cron;
    private volatile boolean running = true;

    public CronFlushStrategy(RandomAccessFile file, String cronExpression) {
        this.file = file;
        this.cronExpression = cronExpression;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WAL-CronFlush");
            t.setDaemon(true);
            return t;
        });
        
        CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
        this.cron = parser.parse(cronExpression);
        
        scheduleNextFlush();
        logger.info("CronFlushStrategy initialized with expression: {}", cronExpression);
    }

    private void scheduleNextFlush() {
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        ZonedDateTime now = ZonedDateTime.now();
        long delayMs = executionTime.timeToNextExecution(now)
            .map(Duration::toMillis)
            .orElse(1000L);
        
        scheduler.schedule(() -> {
            if (running) {
                flush();
                scheduleNextFlush();
            }
        }, delayMs, TimeUnit.MILLISECONDS);
        
        logger.debug("Next flush scheduled in {} ms", delayMs);
    }

    @Override
    public void onWrite() {
        // Cron strategy doesn't need to do anything on write
    }

    @Override
    public void flush() {
        try {
            file.getFD().sync();
            logger.debug("Cron flush completed");
        } catch (IOException e) {
            logger.error("Failed to cron flush", e);
            throw new RuntimeException("Failed to cron flush", e);
        }
    }

    @Override
    public void shutdown() {
        running = false;
        flush();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("CronFlushStrategy shutdown");
    }
}
