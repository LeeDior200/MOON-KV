package com.saki.benchmark;

import com.saki.benchmark.config.BenchmarkScenario;
import com.saki.benchmark.config.BenchmarkConfigLoader;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ConfigurableBenchmarkRunner {

    public static void main(String[] args) throws RunnerException, IOException {
        if (args.length > 0) {
            String command = args[0];

            if ("--list".equals(command)) {
                listScenarios();
                return;
            } else if ("--config".equals(command) && args.length > 1) {
                String configPath = args[1];
                runWithCustomConfig(configPath, args.length > 2 ? args[2] : null);
                return;
            } else if ("--help".equals(command)) {
                printHelp();
                return;
            }
        }

        Options opt = new OptionsBuilder()
                .include(ConfigurableKVStoreBenchmark.class.getSimpleName())
                .warmupIterations(5)
                .warmupTime(TimeValue.seconds(3))
                .measurementIterations(5)
                .measurementTime(TimeValue.seconds(3))
                .forks(3)
                .timeUnit(TimeUnit.SECONDS)
                .mode(org.openjdk.jmh.annotations.Mode.Throughput)
                .build();

        new org.openjdk.jmh.runner.Runner(opt).run();
    }

    private static void listScenarios() throws IOException {
        System.out.println("Available benchmark scenarios:");
        System.out.println("================================");

        List<BenchmarkScenario> scenarios = BenchmarkConfigLoader.loadDefaultScenarios();

        for (int i = 0; i < scenarios.size(); i++) {
            BenchmarkScenario scenario = scenarios.get(i);
            System.out.println((i + 1) + ". " + scenario.getName());
            System.out.println("   Description: " + scenario.getDescription());

            if (scenario.getWalConfig() != null) {
                System.out.println("   WAL Strategy: " + scenario.getWalConfig().getFlushStrategyType());
            }
            if (scenario.getExpiryConfig() != null) {
                System.out.println("   Expiry Strategy: " + scenario.getExpiryConfig().getStrategyType());
            }
            if (scenario.getMemoryConfig() != null) {
                System.out.println("   Memory Strategy: " + scenario.getMemoryConfig().getEvictionStrategy());
            }
            System.out.println();
        }

        System.out.println("Usage:");
        System.out.println("  java -jar moon-kv-test-1.0.0.jar ConfigurableKVStoreBenchmark -p scenarioName=<scenario>");
        System.out.println("  Example: java -jar moon-kv-test-1.0.0.jar ConfigurableKVStoreBenchmark -p scenarioName=sync-lazy-lru");
    }

    private static void runWithCustomConfig(String configPath, String scenarioName) throws IOException, RunnerException {
        System.out.println("Loading custom config from: " + configPath);

        List<BenchmarkScenario> scenarios = BenchmarkConfigLoader.loadScenarios(configPath);

        System.out.println("Loaded " + scenarios.size() + " scenarios from custom config");

        if (scenarioName != null) {
            boolean found = scenarios.stream().anyMatch(s -> s.getName().equals(scenarioName));
            if (!found) {
                System.err.println("Scenario not found: " + scenarioName);
                System.err.println("Available scenarios:");
                scenarios.forEach(s -> System.err.println("  - " + s.getName()));
                return;
            }

            Options opt = new OptionsBuilder()
                    .include(ConfigurableKVStoreBenchmark.class.getSimpleName())
                    .param("scenarioName", scenarioName)
                    .warmupIterations(5)
                    .warmupTime(TimeValue.seconds(3))
                    .measurementIterations(5)
                    .measurementTime(TimeValue.seconds(3))
                    .forks(3)
                    .build();

            new org.openjdk.jmh.runner.Runner(opt).run();
        } else {
            Options opt = new OptionsBuilder()
                    .include(ConfigurableKVStoreBenchmark.class.getSimpleName())
                    .warmupIterations(5)
                    .warmupTime(TimeValue.seconds(3))
                    .measurementIterations(5)
                    .measurementTime(TimeValue.seconds(3))
                    .forks(3)
                    .build();

            new org.openjdk.jmh.runner.Runner(opt).run();
        }
    }

    private static void printHelp() {
        System.out.println("ConfigurableBenchmarkRunner - MOON-KV Performance Testing");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar moon-kv-test-1.0.0.jar ConfigurableBenchmarkRunner [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --list                    List all available benchmark scenarios");
        System.out.println("  --config <path> [name]    Run with custom config file");
        System.out.println("  --help                    Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # List all scenarios");
        System.out.println("  java -jar moon-kv-test-1.0.0.jar ConfigurableBenchmarkRunner --list");
        System.out.println();
        System.out.println("  # Run with custom config");
        System.out.println("  java -jar moon-kv-test-1.0.0.jar ConfigurableBenchmarkRunner --config custom-scenarios.json");
        System.out.println();
        System.out.println("  # Run specific scenario with custom config");
        System.out.println("  java -jar moon-kv-test-1.0.0.jar ConfigurableBenchmarkRunner --config custom-scenarios.json my-scenario");
        System.out.println();
        System.out.println("  # Run all scenarios with default config");
        System.out.println("  java -jar moon-kv-test-1.0.0.jar ConfigurableKVStoreBenchmark");
        System.out.println();
        System.out.println("  # Run specific scenario");
        System.out.println("  java -jar moon-kv-test-1.0.0.jar ConfigurableKVStoreBenchmark -p scenarioName=sync-lazy-lru");
    }
}
