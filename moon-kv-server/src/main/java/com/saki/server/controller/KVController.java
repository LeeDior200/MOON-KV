package com.saki.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saki.engine.KVStore;
import com.saki.server.api.ApiResponse;
import com.saki.server.api.RouteHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KVController implements RouteHandler {
    private static final Logger logger = LoggerFactory.getLogger(KVController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ApiResponse handle(FullHttpRequest request) throws Exception {
        String uri = request.uri();
        int queryIndex = uri.indexOf('?');
        if (queryIndex > 0) {
            uri = uri.substring(0, queryIndex);
        }

        HttpMethod method = request.method();

        if (uri.equals("/api/v1/kv")) {
            if (method == HttpMethod.GET) {
                return handleGetAllKeys(request);
            }
            return ApiResponse.badRequest("Method not allowed");
        }

        Pattern pattern = Pattern.compile("^/api/v1/kv/([^/]+)$");
        Matcher matcher = pattern.matcher(uri);

        if (matcher.matches()) {
            String key = matcher.group(1);

            if (method == HttpMethod.GET) {
                return handleGet(key);
            } else if (method == HttpMethod.PUT) {
                return handlePut(key, request);
            } else if (method == HttpMethod.DELETE) {
                return handleDelete(key);
            }

            return ApiResponse.badRequest("Method not allowed");
        }

        Pattern ttlPattern = Pattern.compile("^/api/v1/kv/([^/]+)/ttl$");
        Matcher ttlMatcher = ttlPattern.matcher(uri);

        if (ttlMatcher.matches()) {
            String key = ttlMatcher.group(1);
            if (method == HttpMethod.POST) {
                return handleSetTtl(key, request);
            }
            return ApiResponse.badRequest("Method not allowed");
        }

        return ApiResponse.notFound("Endpoint not found");
    }

    private ApiResponse handleGet(String key) {
        logger.info("GET key: {}", key);

        String value = KVStore.get(key);
        if (value == null) {
            return ApiResponse.notFound("Key not found: " + key);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("value", value);

        return ApiResponse.success(result);
    }

    private ApiResponse handleGetAllKeys(FullHttpRequest request) {
        logger.info("GET all keys");

        KVStore store = KVStore.getInstance();

        Map<String, Object> result = new HashMap<>();
        result.put("count", 0);
        result.put("keys", Collections.emptyList());

        return ApiResponse.success(result);
    }

    private ApiResponse handlePut(String key, FullHttpRequest request) throws Exception {
        logger.info("PUT key: {}", key);

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

        Object valueObj = body.get("value");
        if (valueObj == null) {
            return ApiResponse.badRequest("Field 'value' is required");
        }

        String value = valueObj.toString();

        Object ttlObj = body.get("ttl");
        if (ttlObj != null) {
            try {
                long ttl = Long.parseLong(ttlObj.toString());
                KVStore.setex(key, value, ttl);
                logger.info("Set key {} with TTL {}s", key, ttl);
            } catch (NumberFormatException e) {
                return ApiResponse.badRequest("Invalid TTL value");
            }
        } else {
            KVStore.set(key, value);
            logger.info("Set key {}", key);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("value", value);
        if (ttlObj != null) {
            result.put("ttl", Long.parseLong(ttlObj.toString()));
        }

        return ApiResponse.success("Key set successfully", result);
    }

    private ApiResponse handleDelete(String key) {
        logger.info("DELETE key: {}", key);

        String value = KVStore.get(key);
        if (value == null) {
            return ApiResponse.notFound("Key not found: " + key);
        }

        KVStore.delete(key);
        logger.info("Deleted key {}", key);

        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("deleted", true);

        return ApiResponse.success("Key deleted successfully", result);
    }

    private ApiResponse handleSetTtl(String key, FullHttpRequest request) throws Exception {
        logger.info("SET TTL for key: {}", key);

        String value = KVStore.get(key);
        if (value == null) {
            return ApiResponse.notFound("Key not found: " + key);
        }

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

        Object ttlObj = body.get("ttl");
        if (ttlObj == null) {
            return ApiResponse.badRequest("Field 'ttl' is required");
        }

        try {
            long ttl = Long.parseLong(ttlObj.toString());
            KVStore.setex(key, value, ttl);
            logger.info("Set TTL {}s for key {}", ttl, key);

            Map<String, Object> result = new HashMap<>();
            result.put("key", key);
            result.put("ttl", ttl);

            return ApiResponse.success("TTL set successfully", result);

        } catch (NumberFormatException e) {
            return ApiResponse.badRequest("Invalid TTL value");
        }
    }
}
