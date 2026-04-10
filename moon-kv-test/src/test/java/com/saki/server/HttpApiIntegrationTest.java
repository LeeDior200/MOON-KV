package com.saki.server;

import com.saki.engine.KVStore;
import com.saki.server.http.HttpServer;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HttpApiIntegrationTest {

    private static HttpServer server;
    private static final int TEST_PORT = 4079;
    private static final String BASE_URL = "http://localhost:" + TEST_PORT;

    @BeforeAll
    static void startServer() {
        System.setProperty("kv.wal.path", "./data/test_http_api.wal");
        
        server = new HttpServer(TEST_PORT);
        server.start();
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop();
        }
        
        try {
            KVStore store = KVStore.getInstance();
            if (store != null) {
                store.close();
            }
        } catch (Exception e) {
        }
    }

    @Test
    @Order(1)
    void testPutKey() throws Exception {
        String url = BASE_URL + "/api/v1/kv/test_key";
        String jsonBody = "{\"value\": \"test_value\"}";
        
        String response = sendRequest(url, "PUT", jsonBody);
        
        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains("\"key\":\"test_key\""));
        assertTrue(response.contains("\"value\":\"test_value\""));
    }

    @Test
    @Order(2)
    void testGetKey() throws Exception {
        String url = BASE_URL + "/api/v1/kv/test_key";
        
        String response = sendRequest(url, "GET", null);
        
        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains("\"key\":\"test_key\""));
        assertTrue(response.contains("\"value\":\"test_value\""));
    }

    @Test
    @Order(3)
    void testGetNonExistentKey() throws Exception {
        String url = BASE_URL + "/api/v1/kv/nonexistent_key";
        
        String response = sendRequest(url, "GET", null);
        
        assertTrue(response.contains("\"success\":false"));
        assertTrue(response.contains("not found"));
    }

    @Test
    @Order(4)
    void testPutKeyWithTTL() throws Exception {
        String url = BASE_URL + "/api/v1/kv/ttl_key";
        String jsonBody = "{\"value\": \"ttl_value\", \"ttl\": 3600}";
        
        String response = sendRequest(url, "PUT", jsonBody);
        
        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains("\"ttl\":3600"));
    }

    @Test
    @Order(5)
    void testDeleteKey() throws Exception {
        String url = BASE_URL + "/api/v1/kv/test_key";
        
        String response = sendRequest(url, "DELETE", null);
        
        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains("\"deleted\":true"));
    }

    @Test
    @Order(6)
    void testDeleteNonExistentKey() throws Exception {
        String url = BASE_URL + "/api/v1/kv/nonexistent_key";
        
        String response = sendRequest(url, "DELETE", null);
        
        assertTrue(response.contains("\"success\":false"));
        assertTrue(response.contains("not found"));
    }

    @Test
    @Order(7)
    void testPutWithInvalidJson() throws Exception {
        String url = BASE_URL + "/api/v1/kv/invalid_key";
        String invalidJson = "{invalid json}";
        
        String response = sendRequest(url, "PUT", invalidJson);
        
        assertTrue(response.contains("\"success\":false"));
        assertTrue(response.contains("Invalid JSON"));
    }

    @Test
    @Order(8)
    void testPutWithMissingValue() throws Exception {
        String url = BASE_URL + "/api/v1/kv/missing_value_key";
        String jsonBody = "{\"other_field\": \"some_value\"}";
        
        String response = sendRequest(url, "PUT", jsonBody);
        
        assertTrue(response.contains("\"success\":false"));
        assertTrue(response.contains("'value' is required"));
    }

    @Test
    @Order(9)
    void testSetTtl() throws Exception {
        KVStore.set("ttl_test_key", "ttl_test_value");
        
        String url = BASE_URL + "/api/v1/kv/ttl_test_key/ttl";
        String jsonBody = "{\"ttl\": 1800}";
        
        String response = sendRequest(url, "POST", jsonBody);
        
        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains("\"ttl\":1800"));
    }

    @Test
    @Order(10)
    void testSetTtlNonExistentKey() throws Exception {
        String url = BASE_URL + "/api/v1/kv/nonexistent_ttl_key/ttl";
        String jsonBody = "{\"ttl\": 1800}";
        
        String response = sendRequest(url, "POST", jsonBody);
        
        assertTrue(response.contains("\"success\":false"));
        assertTrue(response.contains("not found"));
    }

    @Test
    @Order(11)
    void testHealthEndpoint() throws Exception {
        String url = BASE_URL + "/api/v1/health";
        
        String response = sendRequest(url, "GET", null);
        
        assertTrue(response.contains("\"success\":true") || response.contains("\"status\""));
    }

    @Test
    @Order(12)
    void testStatsEndpoint() throws Exception {
        String url = BASE_URL + "/api/v1/stats";
        
        String response = sendRequest(url, "GET", null);
        
        assertNotNull(response);
        assertTrue(response.length() > 0);
    }

    @Test
    @Order(13)
    void testMemoryStatsEndpoint() throws Exception {
        String url = BASE_URL + "/api/v1/stats/memory";
        
        String response = sendRequest(url, "GET", null);
        
        assertNotNull(response);
    }

    @Test
    @Order(14)
    void testConfigEndpoint() throws Exception {
        String url = BASE_URL + "/api/v1/config";
        
        String response = sendRequest(url, "GET", null);
        
        assertNotNull(response);
    }

    @Test
    @Order(15)
    void testConcurrentRequests() throws Exception {
        int threadCount = 10;
        int requestsPerThread = 10;
        Thread[] threads = new Thread[threadCount];
        int[] successCount = {0};
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        String key = "concurrent_" + threadId + "_" + j;
                        String url = BASE_URL + "/api/v1/kv/" + key;
                        String jsonBody = "{\"value\": \"value_" + j + "\"}";
                        
                        String response = sendRequest(url, "PUT", jsonBody);
                        if (response.contains("\"success\":true")) {
                            synchronized (successCount) {
                                successCount[0]++;
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            });
        }
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        System.out.println("Concurrent requests test: " + successCount[0] + "/" + (threadCount * requestsPerThread) + " successful");
        
        assertTrue(successCount[0] >= threadCount * requestsPerThread * 0.9,
            "At least 90% of requests should succeed");
    }

    @Test
    @Order(16)
    void testUnicodeKeyAndValue() throws Exception {
        String url = BASE_URL + "/api/v1/kv/" + java.net.URLEncoder.encode("中文键", StandardCharsets.UTF_8);
        String jsonBody = "{\"value\": \"中文值\"}";
        
        String response = sendRequest(url, "PUT", jsonBody);
        assertTrue(response.contains("\"success\":true"));
        
        response = sendRequest(url, "GET", null);
        assertTrue(response.contains("中文值"));
    }

    @Test
    @Order(17)
    void testSpecialCharactersInValue() throws Exception {
        String url = BASE_URL + "/api/v1/kv/special_chars_key";
        String jsonBody = "{\"value\": \"value with \\\"quotes\\\" and \\\\backslash\"}";
        
        String response = sendRequest(url, "PUT", jsonBody);
        assertTrue(response.contains("\"success\":true"));
    }

    @Test
    @Order(18)
    void testLargeValue() throws Exception {
        String url = BASE_URL + "/api/v1/kv/large_value_key";
        String largeValue = "x".repeat(10000);
        String jsonBody = "{\"value\": \"" + largeValue + "\"}";
        
        String response = sendRequest(url, "PUT", jsonBody);
        assertTrue(response.contains("\"success\":true"));
        
        response = sendRequest(url, "GET", null);
        assertTrue(response.contains(largeValue.substring(0, 100)));
    }

    @Test
    @Order(19)
    void testGetAllKeys() throws Exception {
        String url = BASE_URL + "/api/v1/kv";
        
        String response = sendRequest(url, "GET", null);
        
        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains("\"count\""));
        assertTrue(response.contains("\"keys\""));
    }

    @Test
    @Order(20)
    void testHealthReadyEndpoint() throws Exception {
        String url = BASE_URL + "/api/v1/health/ready";
        
        String response = sendRequest(url, "GET", null);
        
        assertNotNull(response);
    }

    @Test
    @Order(21)
    void testHealthLiveEndpoint() throws Exception {
        String url = BASE_URL + "/api/v1/health/live";
        
        String response = sendRequest(url, "GET", null);
        
        assertNotNull(response);
    }

    private String sendRequest(String urlStr, String method, String body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        if (body != null && (method.equals("PUT") || method.equals("POST"))) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }
        
        int responseCode = conn.getResponseCode();
        
        BufferedReader reader;
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            if (conn.getErrorStream() != null) {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            } else {
                return "{\"success\":false,\"message\":\"HTTP " + responseCode + "\"}";
            }
        }
        
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        return response.toString();
    }
}
