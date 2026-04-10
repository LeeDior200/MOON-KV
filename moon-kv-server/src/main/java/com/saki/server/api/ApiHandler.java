package com.saki.server.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saki.server.controller.ConfigController;
import com.saki.server.controller.HealthController;
import com.saki.server.controller.KVController;
import com.saki.server.controller.StatsController;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApiHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, RouteHandler> routes = new HashMap<>();
    private final Map<Pattern, RouteHandler> patternRoutes = new HashMap<>();

    public ApiHandler() {
        registerRoutes();
    }

    private void registerRoutes() {
        routes.put("/api/v1/health", new HealthController());
        routes.put("/api/v1/health/ready", new HealthController());
        routes.put("/api/v1/health/live", new HealthController());
        routes.put("/api/v1/stats", new StatsController());
        routes.put("/api/v1/stats/memory", new StatsController());
        routes.put("/api/v1/config", new ConfigController());

        patternRoutes.put(Pattern.compile("^/api/v1/kv/([^/]+)$"), new KVController());
        patternRoutes.put(Pattern.compile("^/api/v1/kv/([^/]+)/ttl$"), new KVController());
        patternRoutes.put(Pattern.compile("^/api/v1/kv$"), new KVController());
    }

    public void handle(io.netty.channel.ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            String uri = request.uri();
            int queryIndex = uri.indexOf('?');
            if (queryIndex > 0) {
                uri = uri.substring(0, queryIndex);
            }

            logger.debug("Handling API request: {} {}", request.method(), uri);

            RouteHandler handler = routes.get(uri);
            if (handler == null) {
                for (Map.Entry<Pattern, RouteHandler> entry : patternRoutes.entrySet()) {
                    Matcher matcher = entry.getKey().matcher(uri);
                    if (matcher.matches()) {
                        handler = entry.getValue();
                        break;
                    }
                }
            }

            if (handler == null) {
                sendResponse(ctx, ApiResponse.notFound("API endpoint not found: " + uri));
                return;
            }

            ApiResponse response = handler.handle(request);
            sendResponse(ctx, response);

        } catch (Exception e) {
            logger.error("Error handling API request", e);
            sendResponse(ctx, ApiResponse.internalError("Internal server error: " + e.getMessage()));
        }
    }

    private void sendResponse(io.netty.channel.ChannelHandlerContext ctx, ApiResponse apiResponse) {
        try {
            String json = objectMapper.writeValueAsString(apiResponse);
            FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(apiResponse.getCode()),
                Unpooled.copiedBuffer(json, StandardCharsets.UTF_8)
            );

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization");

            ctx.writeAndFlush(response);

        } catch (Exception e) {
            logger.error("Error sending API response", e);
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Failed to send response");
        }
    }

    private void sendError(io.netty.channel.ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            status,
            Unpooled.copiedBuffer(message, CharsetUtil.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(io.netty.channel.ChannelFutureListener.CLOSE);
    }
}
