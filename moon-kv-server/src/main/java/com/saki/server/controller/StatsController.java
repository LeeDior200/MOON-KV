package com.saki.server.controller;

import com.saki.engine.KVStore;
import com.saki.engine.memory.MemoryStats;
import com.saki.server.api.ApiResponse;
import com.saki.server.api.RouteHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class StatsController implements RouteHandler {
    private static final Logger logger = LoggerFactory.getLogger(StatsController.class);

    @Override
    public ApiResponse handle(FullHttpRequest request) throws Exception {
        String uri = request.uri();
        int queryIndex = uri.indexOf('?');
        if (queryIndex > 0) {
            uri = uri.substring(0, queryIndex);
        }

        HttpMethod method = request.method();

        if (uri.equals("/api/v1/stats")) {
            if (method == HttpMethod.GET) {
                return handleGetStats(request);
            }
            return ApiResponse.badRequest("Method not allowed");
        }

        if (uri.equals("/api/v1/stats/memory")) {
            if (method == HttpMethod.GET) {
                return handleGetMemoryStats(request);
            }
            return ApiResponse.badRequest("Method not allowed");
        }

        return ApiResponse.notFound("Endpoint not found");
    }

    private ApiResponse handleGetStats(FullHttpRequest request) {
        logger.info("GET system stats");

        KVStore store = KVStore.getInstance();
        MemoryStats memoryStats = store.getMemoryStats();

        Map<String, Object> stats = new HashMap<>();
        stats.put("uptime", System.currentTimeMillis());
        stats.put("keyCount", memoryStats.getEntryCount());
        stats.put("memoryUsage", memoryStats.getUsedBytes());
        stats.put("maxMemory", memoryStats.getMaxBytes());
        stats.put("usageRatio", memoryStats.getUsageRatio());

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        Map<String, Object> jvmStats = new HashMap<>();
        jvmStats.put("totalMemory", totalMemory);
        jvmStats.put("freeMemory", freeMemory);
        jvmStats.put("usedMemory", usedMemory);
        jvmStats.put("maxMemory", maxMemory);
        jvmStats.put("usedMemoryMB", usedMemory / (1024 * 1024));
        jvmStats.put("maxMemoryMB", maxMemory / (1024 * 1024));

        stats.put("jvm", jvmStats);

        return ApiResponse.success(stats);
    }

    private ApiResponse handleGetMemoryStats(FullHttpRequest request) {
        logger.info("GET memory stats");

        KVStore store = KVStore.getInstance();
        MemoryStats memoryStats = store.getMemoryStats();

        Map<String, Object> stats = new HashMap<>();
        stats.put("entryCount", memoryStats.getEntryCount());
        stats.put("maxBytes", memoryStats.getMaxBytes());
        stats.put("usedBytes", memoryStats.getUsedBytes());
        stats.put("usageRatio", memoryStats.getUsageRatio());

        return ApiResponse.success(stats);
    }
}
