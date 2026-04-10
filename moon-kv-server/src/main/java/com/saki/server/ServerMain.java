package com.saki.server;

import com.saki.engine.KVStore;
import com.saki.server.http.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerMain {
    private static final Logger logger = LoggerFactory.getLogger(ServerMain.class);
    
    public static void main(String[] args) {
        logger.info("Starting MOON-KV Server...");
        
        try {
            // Initialize KVStore
            KVStore store = KVStore.getInstance();
            logger.info("KVStore initialized successfully");
            
            // Start HTTP Server
            int port = getPort(args);
            HttpServer server = new HttpServer(port);
            server.start();
            
            logger.info("MOON-KV Server started successfully");
            logger.info("HTTP API: http://localhost:{}", port);
            logger.info("Dashboard: http://localhost:{}/", port);
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down MOON-KV Server...");
                server.stop();
                store.close();
                logger.info("MOON-KV Server stopped");
            }));
            
        } catch (Exception e) {
            logger.error("Failed to start MOON-KV Server", e);
            System.exit(1);
        }
    }
    
    private static int getPort(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                return Integer.parseInt(args[i + 1]);
            }
        }
        return 4070;
    }
}
