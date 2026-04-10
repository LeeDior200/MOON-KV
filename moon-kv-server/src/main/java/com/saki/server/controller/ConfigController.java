package com.saki.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saki.engine.KVStore;
import com.saki.server.api.ApiResponse;
import com.saki.server.api.RouteHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ConfigController implements RouteHandler {
    private static final Logger logger = LoggerFactory.getLogger(ConfigController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ApiResponse handle(FullHttpRequest request) throws Exception {
        String uri = request.uri();
        int queryIndex = uri.indexOf('?');
        if (queryIndex > 0) {
            uri = uri.substring(0, queryIndex);
        }

        HttpMethod method = request.method();

        if (uri.equals("/api/v1/config")) {
            if (method == HttpMethod.GET) {
                return handleGetConfig(request);
            } else if (method == HttpMethod.PUT) {
                return handleUpdateConfig(request);
            }
            return ApiResponse.badRequest("Method not allowed");
        }

        return ApiResponse.notFound("Endpoint not found");
    }

    private ApiResponse handleGetConfig(FullHttpRequest request) {
        logger.info("GET config");

        Map<String, Object> config = new HashMap<>();

        Map<String, Object> serverConfig = new HashMap<>();
        serverConfig.put("port", 8080);
        serverConfig.put("host", "0.0.0.0");
        config.put("server", serverConfig);

        Map<String, Object> walConfig = new HashMap<>();
        walConfig.put("path", System.getProperty("kv.wal.path", "./data/kv_store.wal"));
        walConfig.put("flushStrategy", "ASYNC");
        config.put("wal", walConfig);

        Map<String, Object> expiryConfig = new HashMap<>();
        expiryConfig.put("strategy", "HYBRID");
        expiryConfig.put("checkInterval", 60000);
        expiryConfig.put("lazyEnabled", true);
        config.put("expiry", expiryConfig);

        Map<String, Object> memoryConfig = new HashMap<>();
        memoryConfig.put("maxEntries", 10000);
        memoryConfig.put("evictionStrategy", "LRU");
        config.put("memory", memoryConfig);

        return ApiResponse.success(config);
    }

    private ApiResponse handleUpdateConfig(FullHttpRequest request) throws Exception {
        logger.info("PUT config");

        String content = request.content().toString(StandardCharsets.UTF_8);
        if (content == null || content.isEmpty()) {
            return ApiResponse.badRequest("Request body is required");
        }

        Map<String, Object> body;
        try {
            body = objectMapper.readValue(content, Map.class);
        } catch (Exception e) {
            logger.error("Failed to parse request body", e);
            return ApiResponse.badRequest("Invalid JSON format");
        }

        logger.info("Config update requested: {}", body);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Configuration update is not supported in this version");
        result.put("requested", body);

        return ApiResponse.success(result);
    }
}
