package com.saki.server.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {
    private int code;
    private String message;
    private Object data;
    private long timestamp;

    public ApiResponse() {
        this.timestamp = Instant.now().toEpochMilli();
    }

    public ApiResponse(int code, String message) {
        this();
        this.code = code;
        this.message = message;
    }

    public ApiResponse(int code, String message, Object data) {
        this(code, message);
        this.data = data;
    }

    public static ApiResponse success() {
        return new ApiResponse(200, "Success");
    }

    public static ApiResponse success(Object data) {
        return new ApiResponse(200, "Success", data);
    }

    public static ApiResponse success(String message, Object data) {
        return new ApiResponse(200, message, data);
    }

    public static ApiResponse error(int code, String message) {
        return new ApiResponse(code, message);
    }

    public static ApiResponse error(int code, String message, Object data) {
        return new ApiResponse(code, message, data);
    }

    public static ApiResponse badRequest(String message) {
        return new ApiResponse(400, message);
    }

    public static ApiResponse notFound(String message) {
        return new ApiResponse(404, message);
    }

    public static ApiResponse internalError(String message) {
        return new ApiResponse(500, message);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
