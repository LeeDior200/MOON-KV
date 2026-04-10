package com.saki.benchmark.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class BenchmarkConfigLoader {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DEFAULT_CONFIG_PATH = "benchmark/scenarios.json";

    public static List<BenchmarkScenario> loadScenarios(String configPath) throws IOException {
        File file = new File(configPath);
        if (file.exists()) {
            return loadFromFile(file);
        } else {
            return loadFromClasspath(configPath);
        }
    }

    public static List<BenchmarkScenario> loadDefaultScenarios() throws IOException {
        return loadFromClasspath(DEFAULT_CONFIG_PATH);
    }

    private static List<BenchmarkScenario> loadFromFile(File file) throws IOException {
        Map<String, Object> config = objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {});
        return parseScenarios(config);
    }

    private static List<BenchmarkScenario> loadFromClasspath(String path) throws IOException {
        InputStream inputStream = BenchmarkConfigLoader.class.getClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            throw new IOException("Configuration file not found: " + path);
        }

        Map<String, Object> config = objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
        return parseScenarios(config);
    }

    @SuppressWarnings("unchecked")
    private static List<BenchmarkScenario> parseScenarios(Map<String, Object> config) {
        List<BenchmarkScenario> scenarios = new ArrayList<>();

        List<Map<String, Object>> scenarioList = (List<Map<String, Object>>) config.get("scenarios");
        if (scenarioList == null) {
            return scenarios;
        }

        for (Map<String, Object> scenarioMap : scenarioList) {
            BenchmarkScenario scenario = new BenchmarkScenario();

            scenario.setName((String) scenarioMap.get("name"));
            scenario.setDescription((String) scenarioMap.get("description"));

            Map<String, Object> walConfig = (Map<String, Object>) scenarioMap.get("wal");
            if (walConfig != null) {
                scenario.setWalConfig(parseWalConfig(walConfig));
            }

            Map<String, Object> expiryConfig = (Map<String, Object>) scenarioMap.get("expiry");
            if (expiryConfig != null) {
                scenario.setExpiryConfig(parseExpiryConfig(expiryConfig));
            }

            Map<String, Object> memoryConfig = (Map<String, Object>) scenarioMap.get("memory");
            if (memoryConfig != null) {
                scenario.setMemoryConfig(parseMemoryConfig(memoryConfig));
            }

            Map<String, Object> benchmarkParams = (Map<String, Object>) scenarioMap.get("benchmark");
            if (benchmarkParams != null) {
                scenario.setBenchmarkParams(parseBenchmarkParams(benchmarkParams));
            }

            scenarios.add(scenario);
        }

        return scenarios;
    }

    private static BenchmarkScenario.WalConfig parseWalConfig(Map<String, Object> config) {
        BenchmarkScenario.WalConfig walConfig = new BenchmarkScenario.WalConfig();

        if (config.containsKey("flushStrategy")) {
            walConfig.setFlushStrategyType((String) config.get("flushStrategy"));
        }
        if (config.containsKey("flushIntervalMs")) {
            walConfig.setFlushIntervalMs(((Number) config.get("flushIntervalMs")).longValue());
        }
        if (config.containsKey("batchSize")) {
            walConfig.setBatchSize(((Number) config.get("batchSize")).intValue());
        }

        return walConfig;
    }

    private static BenchmarkScenario.ExpiryConfig parseExpiryConfig(Map<String, Object> config) {
        BenchmarkScenario.ExpiryConfig expiryConfig = new BenchmarkScenario.ExpiryConfig();

        if (config.containsKey("strategy")) {
            expiryConfig.setStrategyType((String) config.get("strategy"));
        }
        if (config.containsKey("scanIntervalMs")) {
            expiryConfig.setScanIntervalMs(((Number) config.get("scanIntervalMs")).longValue());
        }
        if (config.containsKey("scanBatchSize")) {
            expiryConfig.setScanBatchSize(((Number) config.get("scanBatchSize")).intValue());
        }

        return expiryConfig;
    }

    private static BenchmarkScenario.MemoryConfig parseMemoryConfig(Map<String, Object> config) {
        BenchmarkScenario.MemoryConfig memoryConfig = new BenchmarkScenario.MemoryConfig();

        if (config.containsKey("evictionStrategy")) {
            memoryConfig.setEvictionStrategy((String) config.get("evictionStrategy"));
        }
        if (config.containsKey("maxSizeMB")) {
            memoryConfig.setMaxSizeMB(((Number) config.get("maxSizeMB")).intValue());
        }
        if (config.containsKey("evictionRatio")) {
            memoryConfig.setEvictionRatio(((Number) config.get("evictionRatio")).doubleValue());
        }

        return memoryConfig;
    }

    private static BenchmarkScenario.BenchmarkParams parseBenchmarkParams(Map<String, Object> config) {
        BenchmarkScenario.BenchmarkParams params = new BenchmarkScenario.BenchmarkParams();

        if (config.containsKey("warmupIterations")) {
            params.setWarmupIterations(((Number) config.get("warmupIterations")).intValue());
        }
        if (config.containsKey("warmupTime")) {
            params.setWarmupTime(((Number) config.get("warmupTime")).intValue());
        }
        if (config.containsKey("measurementIterations")) {
            params.setMeasurementIterations(((Number) config.get("measurementIterations")).intValue());
        }
        if (config.containsKey("measurementTime")) {
            params.setMeasurementTime(((Number) config.get("measurementTime")).intValue());
        }
        if (config.containsKey("forks")) {
            params.setForks(((Number) config.get("forks")).intValue());
        }
        if (config.containsKey("threads")) {
            params.setThreads(((Number) config.get("threads")).intValue());
        }

        return params;
    }

    public static BenchmarkScenario findScenario(List<BenchmarkScenario> scenarios, String name) {
        return scenarios.stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public static void listScenarios(List<BenchmarkScenario> scenarios) {
        System.out.println("Available benchmark scenarios:");
        System.out.println("================================");
        for (BenchmarkScenario scenario : scenarios) {
            System.out.println("\nName: " + scenario.getName());
            System.out.println("Description: " + scenario.getDescription());
            if (scenario.getWalConfig() != null) {
                System.out.println("WAL Strategy: " + scenario.getWalConfig().getFlushStrategyType());
            }
            if (scenario.getExpiryConfig() != null) {
                System.out.println("Expiry Strategy: " + scenario.getExpiryConfig().getStrategyType());
            }
            if (scenario.getMemoryConfig() != null) {
                System.out.println("Memory Strategy: " + scenario.getMemoryConfig().getEvictionStrategy());
            }
        }
    }
}
