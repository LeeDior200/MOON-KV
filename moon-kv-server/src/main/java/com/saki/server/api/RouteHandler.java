package com.saki.server.api;

import io.netty.handler.codec.http.FullHttpRequest;

public interface RouteHandler {
    ApiResponse handle(FullHttpRequest request) throws Exception;
}
