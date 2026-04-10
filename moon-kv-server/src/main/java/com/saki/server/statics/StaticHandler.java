package com.saki.server.statics;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class StaticHandler {
    private static final Logger logger = LoggerFactory.getLogger(StaticHandler.class);

    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put(".html", "text/html; charset=UTF-8");
        MIME_TYPES.put(".htm", "text/html; charset=UTF-8");
        MIME_TYPES.put(".css", "text/css; charset=UTF-8");
        MIME_TYPES.put(".js", "application/javascript; charset=UTF-8");
        MIME_TYPES.put(".json", "application/json; charset=UTF-8");
        MIME_TYPES.put(".xml", "application/xml; charset=UTF-8");
        MIME_TYPES.put(".txt", "text/plain; charset=UTF-8");
        MIME_TYPES.put(".png", "image/png");
        MIME_TYPES.put(".jpg", "image/jpeg");
        MIME_TYPES.put(".jpeg", "image/jpeg");
        MIME_TYPES.put(".gif", "image/gif");
        MIME_TYPES.put(".svg", "image/svg+xml");
        MIME_TYPES.put(".ico", "image/x-icon");
        MIME_TYPES.put(".woff", "font/woff");
        MIME_TYPES.put(".woff2", "font/woff2");
        MIME_TYPES.put(".ttf", "font/ttf");
        MIME_TYPES.put(".eot", "application/vnd.ms-fontobject");
    }

    public void handle(io.netty.channel.ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            String uri = request.uri();
            int queryIndex = uri.indexOf('?');
            if (queryIndex > 0) {
                uri = uri.substring(0, queryIndex);
            }

            if ("/".equals(uri)) {
                uri = "/index.html";
            }

            logger.debug("Handling static request: {}", uri);

            String resourcePath = "static" + uri;
            byte[] content = loadResource(resourcePath);

            if (content == null) {
                logger.warn("Static resource not found: {}", resourcePath);
                sendError(ctx, HttpResponseStatus.NOT_FOUND, "Resource not found: " + uri);
                return;
            }

            String mimeType = getMimeType(uri);
            sendStaticResponse(ctx, content, mimeType);

        } catch (Exception e) {
            logger.error("Error handling static request", e);
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    private byte[] loadResource(String resourcePath) {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (inputStream == null) {
                return null;
            }

            byte[] content = inputStream.readAllBytes();
            inputStream.close();
            return content;

        } catch (Exception e) {
            logger.error("Failed to load resource: {}", resourcePath, e);
            return null;
        }
    }

    private String getMimeType(String path) {
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex > 0) {
            String extension = path.substring(dotIndex).toLowerCase();
            String mimeType = MIME_TYPES.get(extension);
            if (mimeType != null) {
                return mimeType;
            }
        }
        return "application/octet-stream";
    }

    private void sendStaticResponse(io.netty.channel.ChannelHandlerContext ctx, byte[] content, String mimeType) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK,
            Unpooled.copiedBuffer(content)
        );

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeType);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.length);
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "max-age=3600");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

        ctx.writeAndFlush(response);
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
