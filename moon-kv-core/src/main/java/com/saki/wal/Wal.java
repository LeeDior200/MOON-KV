package com.saki.wal;

import com.saki.wal.config.WalConfig;
import com.saki.wal.strategy.FlushStrategy;
import com.saki.wal.strategy.FlushStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class Wal implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(Wal.class);
    
    private final RandomAccessFile file;
    private final FlushStrategy flushStrategy;
    private final WalConfig config;

    public Wal(String filePath) {
        this(filePath, new WalConfig());
    }

    public Wal(String filePath, WalConfig config) {
        this.config = config;
        try {
            File f = new File(filePath);
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            this.file = new RandomAccessFile(f, "rw");
            this.file.seek(this.file.length());
            this.flushStrategy = FlushStrategyFactory.createStrategy(config, file);
            
            logger.info("WAL initialized at {} with strategy {}", filePath, config.getFlushStrategyType());
        } catch (IOException e) {
            logger.error("Failed to open WAL: {}", filePath, e);
            throw new RuntimeException("Failed to open WAL: " + filePath, e);
        }
    }

    public void write(String operation, String key, String value) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(operation).append("|").append(key);
            if (value != null) {
                sb.append("|").append(value);
            }
            sb.append("\n");

            file.write(sb.toString().getBytes());
            flushStrategy.onWrite();
            
            logger.debug("WAL write: {} {}", operation, key);
        } catch (IOException e) {
            logger.error("Failed to write WAL", e);
            throw new RuntimeException("Failed to write WAL", e);
        }
    }

    public void replay(LogHandler handler) {
        try {
            file.seek(0);
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file.getFD())));
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length < 2) {
                    continue;
                }
                String value = null;
                if (parts.length >= 3) {
                    StringBuilder valueBuilder = new StringBuilder(parts[2]);
                    for (int i = 3; i < parts.length; i++) {
                        valueBuilder.append("|").append(parts[i]);
                    }
                    value = valueBuilder.toString();
                }
                handler.handle(parts[0], parts[1], value);
                count++;
            }
            logger.info("WAL replay completed, {} records processed", count);
        } catch (IOException e) {
            logger.error("Failed to replay WAL", e);
            throw new RuntimeException("Failed to replay WAL", e);
        }
    }

    public void truncate() {
        try {
            file.setLength(0);
            logger.info("WAL truncated");
        } catch (IOException e) {
            logger.error("Failed to truncate WAL", e);
            throw new RuntimeException("Failed to truncate WAL", e);
        }
    }

    public void flush() {
        flushStrategy.flush();
        logger.debug("WAL manual flush");
    }

    @Override
    public void close() {
        try {
            flushStrategy.shutdown();
            file.close();
            logger.info("WAL closed");
        } catch (IOException e) {
            logger.error("Failed to close WAL file", e);
            throw new RuntimeException("Failed to close WAL file", e);
        }
    }

    public interface LogHandler {
        void handle(String operation, String key, String value);
    }
}
