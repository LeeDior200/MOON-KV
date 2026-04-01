package com.saki.wal;


import java.io.*;

public class Wal implements AutoCloseable{
    private final RandomAccessFile file;

    public Wal(String filePath) {
        try {
            File f = new File(filePath);
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            this.file = new RandomAccessFile(f, "rw");
            this.file.seek(this.file.length()); // 追加模式
        } catch (IOException e) {
            throw new RuntimeException("Failed to open WAL: " + filePath, e);
        }
    }

    // 写入一条日志
    public void write(String operation, String key, String value) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(operation).append("|").append(key);
            if (value != null) {
                sb.append("|").append(value);
            }
            sb.append("\n");

            file.write(sb.toString().getBytes());
            //TODO 待优化
            file.getFD().sync();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write WAL", e);
        }
    }

    // 读取所有日志（逐行返回）
    public void replay(LogHandler handler) {
        try {
            file.seek(0); // 回到文件开头
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file.getFD())));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length < 2) {
                    continue;
                }
                handler.handle(parts[0], parts[1], parts.length >= 3 ? parts[2] : null);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to replay WAL", e);
        }
    }

    // 截断文件（清空或缩小）
    public void truncate() {
        try {
            file.setLength(0);
        } catch (IOException e) {
            throw new RuntimeException("Failed to truncate WAL", e);
        }
    }
    @Override
    public void close() {
        try {
            file.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }
    // 回调接口
    public interface LogHandler {
        void handle(String operation, String key, String value);
    }
}
