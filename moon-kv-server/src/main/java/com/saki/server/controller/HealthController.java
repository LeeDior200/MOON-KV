package com.saki.server.controller;

import com.saki.engine.KVStore;
import com.saki.server.api.ApiResponse;
import com.saki.server.api.RouteHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;

public class HealthController implements RouteHandler {
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    private static final long START_TIME = System.currentTimeMillis();
    private static final String VERSION = "1.0.0";

    @Override
    public ApiResponse handle(FullHttpRequest request) throws Exception {
        String uri = request.uri();
        int queryIndex = uri.indexOf('?');
        if (queryIndex > 0) {
            uri = uri.substring(0, queryIndex);
        }

        HttpMethod method = request.method();

        if (uri.equals("/api/v1/health")) {
            if (method == HttpMethod.GET) {
                return handleHealthCheck(request);
            }
            return ApiResponse.badRequest("Method not allowed");
        }

        if (uri.equals("/api/v1/health/ready")) {
            if (method == HttpMethod.GET) {
                return handleReadyCheck(request);
            }
            return ApiResponse.badRequest("Method not allowed");
        }

        if (uri.equals("/api/v1/health/live")) {
            if (method == HttpMethod.GET) {
                return handleLiveCheck(request);
            }
            return ApiResponse.badRequest("Method not allowed");
        }

        return ApiResponse.notFound("Endpoint not found");
    }

    private ApiResponse handleHealthCheck(FullHttpRequest request) {
        logger.debug("Health check requested");

        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        long uptime = runtimeBean.getUptime();

        KVStore store = KVStore.getInstance();
        boolean isHealthy = store != null;

        Map<String, Object> health = new HashMap<>();
        health.put("status", isHealthy ? "UP" : "DOWN");
        health.put("version", VERSION);
        health.put("uptime", uptime);
        health.put("uptimeFormatted", formatUptime(uptime));

        Map<String, Object> components = new HashMap<>();

        Map<String, Object> kvStore = new HashMap<>();
        kvStore.put("status", isHealthy ? "UP" : "DOWN");
        kvStore.put("type", "KVStore");
        components.put("kvStore", kvStore);

        Map<String, Object> memory = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;

        memory.put("status", memoryUsagePercent < 90 ? "UP" : "WARNING");
        memory.put("maxMemory", maxMemory);
        memory.put("usedMemory", usedMemory);
        memory.put("usagePercent", String.format("%.2f%%", memoryUsagePercent));
        components.put("memory", memory);

        health.put("components", components);

        return ApiResponse.success(health);
    }

    private ApiResponse handleReadyCheck(FullHttpRequest request) {
        logger.debug("Readiness check requested");

        KVStore store = KVStore.getInstance();
        boolean isReady = store != null;

        Map<String, Object> ready = new HashMap<>();
        ready.put("ready", isReady);

        if (isReady) {
            return ApiResponse.success(ready);
        } else {
            return ApiResponse.error(503, "Service not ready", ready);
        }
    }

    private ApiResponse handleLiveCheck(FullHttpRequest request) {
        logger.debug("Liveness check requested");

        Map<String, Object> live = new HashMap<>();
        live.put("alive", true);

        return ApiResponse.success(live);
    }

    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%d days, %d hours, %d minutes", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%d hours, %d minutes", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d minutes, %d seconds", minutes, seconds % 60);
        } else {
            return String.format("%d seconds", seconds);
        }
    }
}
