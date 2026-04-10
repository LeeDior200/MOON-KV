# MOON-KV

<div align="center">

[![Java Version](https://img.shields.io/badge/Java-17%2B-blue)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.6%2B-orange)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

**一个轻量级的键值存储数据库**

[English](#english) | [中文](#中文)

</div>

---

## 中文

### 📖 项目简介

MOON-KV是一个基于Java的轻量级键值存储数据库，支持WAL持久化、TTL过期机制和内存管理。项目采用模块化架构设计，提供完整的HTTP REST API和Web管理界面，适合中小型应用、嵌入式场景和学习研究。

**轻量化评分**: ⭐⭐⭐⭐⭐ (9.2/10)

### ✨ 核心特性

#### 存储引擎
- 🔹 **KV存储** - 基于ConcurrentHashMap的高性能键值存储
- 🔹 **WAL持久化** - 支持SYNC/ASYNC/BATCH/CRON多种刷盘策略
- 🔹 **TTL过期** - 支持LAZY/PERIODIC/HYBRID三种过期策略
- 🔹 **内存管理** - 支持LRU/LFU/FIFO三种淘汰策略

#### 服务功能
- 🔹 **HTTP服务器** - 基于Netty的高性能HTTP服务
- 🔹 **REST API** - 12个RESTful API端点
- 🔹 **Web Dashboard** - 现代化的Web管理界面
- 🔹 **健康检查** - 完善的健康检查机制

#### 运维支持
- 🔹 **跨平台** - 支持Windows/Linux/macOS
- 🔹 **配置管理** - 灵活的配置文件管理
- 🔹 **日志管理** - 完善的日志系统
- 🔹 **发布包** - 一键部署发布包

### 📊 性能指标

#### 基础指标

| 指标 | 数值 | 说明 |
|------|------|------|
| 启动时间 | < 2秒 | 快速启动 |
| 内存占用 | ~100MB | 轻量级内存占用 |
| 发布包大小 | 9.82MB | 小巧的发布包 |
| API响应时间 | < 100ms | 快速响应 |
| 并发能力 | 100+ QPS | 良好的并发性能 |
| 代码规模 | 3,867行 | 易于理解和维护 |

#### 性能测试结果

**测试环境**：
- 测试时间：2026-04-11
- 测试配置：5 个场景，每个场景 5 个测试方法
- 预热轮次：5 轮 × 3 秒（推荐值，5轮以上JIT才能稳定）
- 测试轮次：5 轮 × 3 秒
- Fork 数：3（推荐值，避免受OS干扰）

**测试场景**：
1. **sync-lazy-lru**：SYNC刷盘 + LAZY过期 + LRU淘汰 - 数据安全优先
2. **async-periodic-lfu**：ASYNC刷盘 + PERIODIC过期 + LFU淘汰 - 性能优先
3. **batch-hybrid-lru**：BATCH刷盘 + HYBRID过期 + LRU淘汰 - 平衡模式
4. **batch-lazy-fifo**：BATCH刷盘 + LAZY过期 + FIFO淘汰 - 高吞吐量
5. **async-hybrid-lru**：ASYNC刷盘 + HYBRID过期 + LRU淘汰 - 推荐配置

**性能数据摘要**：

| 操作类型 | SYNC策略 | ASYNC/BATCH策略 | 性能差距 |
|---------|---------|----------------|---------|
| 写入 | 1,636 ops/s | 3,900-4,200 ops/s | 2-3倍 |
| 读取 | 12,700 ops/s | 13,000-13,700 ops/s | 相同 |
| 删除 | 不稳定 | 6,000-6,200 ops/s | - |
| 混合读写 | 12,700 ops/s | 13,000-13,700 ops/s | 相同 |

**推荐配置**：
- 🥇 **最佳性能**：batch-lazy-fifo - 高吞吐量场景
- 🥈 **平衡推荐**：async-hybrid-lru - 通用场景
- 🥉 **数据安全**：sync-lazy-lru - 仅当数据安全极其重要时

详细性能测试报告请查看 [moon-kv-test 模块](moon-kv-test/README.md)。

### 🚀 快速开始

#### 环境要求
- Java 17 或更高版本
- Maven 3.6+ (仅构建时需要)

#### 方式一：使用发布包（推荐）

1. **下载发布包**
```bash
# 下载最新的发布包
# moon-kv-server-1.0.0-dist.tar.gz (Linux/Mac)
# moon-kv-server-1.0.0-dist.zip (Windows)
```

2. **解压并启动**
```bash
# Linux/Mac
tar -xzf moon-kv-server-1.0.0-dist.tar.gz
cd moon-kv-server-1.0.0
./bin/start.sh

# Windows
# 解压 moon-kv-server-1.0.0-dist.zip
cd moon-kv-server-1.0.0
bin\start.bat
```

3. **访问Dashboard**
```
浏览器打开: http://localhost:4070
```

#### 方式二：从源码构建

1. **克隆项目**
```bash
git clone https://github.com/your-username/moon-kv.git
cd moon-kv
```

2. **构建项目**
```bash
mvn clean package
```

3. **运行服务**
```bash
java -jar moon-kv-server/target/moon-kv-server-1.0.0.jar
```

#### 方式三：嵌入式使用

```java
import com.saki.engine.KVStore;
import com.saki.wal.config.WalConfig;
import com.saki.engine.expiry.ExpiryConfig;
import com.saki.engine.memory.MemoryConfig;

// 使用默认配置
KVStore.set("key", "value");
String value = KVStore.get("key");

// 设置带TTL的键值
KVStore.setex("key", "value", 3600); // 3600秒后过期

// 删除键
KVStore.delete("key");

// 使用自定义配置
WalConfig walConfig = new WalConfig();
walConfig.setFlushStrategyType(FlushStrategyType.ASYNC);

ExpiryConfig expiryConfig = new ExpiryConfig();
expiryConfig.setStrategyType(ExpiryStrategyType.HYBRID);

MemoryConfig memoryConfig = new MemoryConfig();
memoryConfig.setMaxEntries(10000);
memoryConfig.setEvictionStrategy(EvictionStrategyType.LRU);

KVStore store = KVStore.getInstance(walConfig, expiryConfig, memoryConfig);
```

### 📚 API文档

#### KV操作API

```bash
# 设置键值
curl -X PUT http://localhost:4070/api/v1/kv/mykey \
  -H "Content-Type: application/json" \
  -d '{"value": "myvalue"}'

# 设置带TTL的键值
curl -X PUT http://localhost:4070/api/v1/kv/mykey \
  -H "Content-Type: application/json" \
  -d '{"value": "myvalue", "ttl": 3600}'

# 获取键值
curl http://localhost:4070/api/v1/kv/mykey

# 获取所有键
curl http://localhost:4070/api/v1/kv

# 删除键
curl -X DELETE http://localhost:4070/api/v1/kv/mykey

# 设置TTL
curl -X POST http://localhost:4070/api/v1/kv/mykey/ttl \
  -H "Content-Type: application/json" \
  -d '{"ttl": 3600}'
```

#### 统计信息API

```bash
# 获取系统统计信息
curl http://localhost:4070/api/v1/stats

# 获取内存统计信息
curl http://localhost:4070/api/v1/stats/memory
```

#### 配置管理API

```bash
# 获取配置
curl http://localhost:4070/api/v1/config

# 更新配置
curl -X PUT http://localhost:4070/api/v1/config \
  -H "Content-Type: application/json" \
  -d '{"key": "value"}'
```

#### 健康检查API

```bash
# 健康检查
curl http://localhost:4070/api/v1/health

# 就绪检查
curl http://localhost:4070/api/v1/health/ready

# 存活检查
curl http://localhost:4070/api/v1/health/live
```

### ⚙️ 配置说明

配置文件位于 `config/server.properties`：

```properties
# Server Configuration
server.port=4070
server.host=0.0.0.0

# WAL Configuration
wal.path=./data/kv_store.wal
wal.flush.strategy=ASYNC
wal.flush.batch.size=1000
wal.flush.cron.expression=0 */5 * * * ?

# Expiry Configuration
expiry.strategy=HYBRID
expiry.check.interval=60000
expiry.lazy.enabled=true

# Memory Configuration
memory.max.entries=10000
memory.eviction.strategy=LRU

# Logging Configuration
log.level=INFO
log.path=./logs
```

#### WAL刷盘策略

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| SYNC | 同步刷盘，每次写入都刷盘 | 数据安全性要求高 |
| ASYNC | 异步刷盘，后台线程定期刷盘 | 性能优先场景 |
| BATCH | 批量刷盘，累积一定数量后刷盘 | 高吞吐场景 |
| CRON | 定时刷盘，按Cron表达式刷盘 | 特定时间点刷盘 |

#### TTL过期策略

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| LAZY | 懒删除，访问时检查过期 | 读多写少场景 |
| PERIODIC | 定期删除，后台线程定期清理 | 写多读少场景 |
| HYBRID | 混合策略，结合懒删除和定期删除 | 通用场景（推荐） |

#### 内存淘汰策略

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| LRU | 最近最少使用 | 通用场景（推荐） |
| LFU | 最不经常使用 | 访问频率差异大 |
| FIFO | 先进先出 | 简单场景 |

### 🏗️ 架构设计

#### 模块结构

```
MOON-KV
├── moon-kv-core          # 核心模块
│   ├── KVStore           # 存储引擎
│   ├── Wal               # WAL持久化
│   ├── ExpiryManager     # TTL过期管理
│   └── MemoryManager     # 内存管理
└── moon-kv-server        # 服务模块
    ├── HttpServer        # HTTP服务器
    ├── ApiHandler        # API处理器
    ├── Controller        # REST控制器
    └── StaticHandler     # 静态资源服务
```

#### 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 编程语言 | Java | 17 |
| 构建工具 | Maven | - |
| 网络框架 | Netty | 4.1.100.Final |
| JSON处理 | Jackson | 2.15.2 |
| 日志框架 | SLF4J + Logback | 2.0.9 + 1.4.11 |
| 工具库 | Guava | 32.1.2-jre |

### 📁 项目结构

```
moon-kv-1.0.0/
├── bin/                  # 启动脚本
│   ├── start.sh         # Linux/Mac启动脚本
│   ├── start.bat        # Windows启动脚本
│   ├── stop.sh          # Linux/Mac停止脚本
│   └── stop.bat         # Windows停止脚本
├── lib/                  # JAR文件
├── config/               # 配置文件
│   ├── server.properties
│   └── logback.xml
├── logs/                 # 日志目录
├── data/                 # 数据目录
└── README.txt
```

### 🎯 适用场景

#### ✅ 推荐使用

- **中小型应用** - 数据量 < 100万条，并发量 < 1000 QPS
- **嵌入式场景** - 嵌入到Java应用中的轻量级KV存储
- **学习研究** - 学习KV存储、WAL持久化、TTL过期机制
- **原型开发** - 快速原型验证和临时数据存储

#### ❌ 不推荐使用

- **大规模生产** - 数据量 > 1000万条，建议使用Redis
- **复杂数据结构** - 需要范围查询、排序等复杂功能
- **分布式场景** - 需要数据分片、主从复制、集群支持

### 🔧 开发指南

#### 构建项目

```bash
# 完整构建
mvn clean package

# 跳过测试
mvn clean package -DskipTests

# 只构建核心模块
cd moon-kv-core
mvn clean package
```

#### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试
mvn test -Dtest=KVStoreTest
```

#### 运行性能测试

```bash
# 编译打包
mvn clean package -DskipTests

# 运行单个场景测试（约 5 分钟）
java -jar moon-kv-test/target/moon-kv-test-1.0.0.jar ConfigurableKVStoreBenchmark -p scenarioName=async-hybrid-lru

# 运行所有场景测试（约 25-30 分钟）
java -jar moon-kv-test/target/moon-kv-test-1.0.0.jar ConfigurableKVStoreBenchmark

# 列出所有可用场景
java -cp moon-kv-test/target/moon-kv-test-1.0.0.jar com.saki.benchmark.ConfigurableBenchmarkRunner --list

# 使用快速测试配置（约 1 分钟）
java -cp moon-kv-test/target/moon-kv-test-1.0.0.jar com.saki.benchmark.ConfigurableBenchmarkRunner --config benchmark/quick-test.json
```

**测试参数说明**：
- **预热轮次**：5 轮 × 3 秒（推荐值，5轮以上JIT才能稳定）
- **测试轮次**：5 轮 × 3 秒
- **Fork 数**：3（推荐值，避免受OS干扰）

**测试场景**：
- **sync-lazy-lru**：SYNC刷盘 + LAZY过期 + LRU淘汰
- **async-periodic-lfu**：ASYNC刷盘 + PERIODIC过期 + LFU淘汰
- **batch-hybrid-lru**：BATCH刷盘 + HYBRID过期 + LRU淘汰
- **batch-lazy-fifo**：BATCH刷盘 + LAZY过期 + FIFO淘汰
- **async-hybrid-lru**：ASYNC刷盘 + HYBRID过期 + LRU淘汰（推荐）

#### 代码规范

- 遵循Java命名规范
- 使用SLF4J进行日志记录
- 异常处理要完善
- 添加必要的注释

### 📈 性能优化建议

#### 基于性能测试结果的优化建议

1. **避免使用 SYNC 刷盘策略**（除非对数据安全有极高要求）
   - 写入性能：1,636 ops/s（最差）
   - 删除性能：不稳定
   - 性能差距：比 ASYNC/BATCH 慢 2-3 倍

2. **推荐使用 ASYNC 或 BATCH 策略**
   - 写入性能：3,900-4,200 ops/s
   - 读取性能：13,000-13,700 ops/s
   - 删除性能：6,000-6,200 ops/s
   - 平衡了性能和数据安全

3. **读取性能不受刷盘策略影响**
   - 所有场景读取性能相近（12,700-13,700 ops/s）
   - 纯内存操作，性能优秀

4. **删除性能优于写入性能**
   - 删除操作更快
   - ASYNC/BATCH 场景下可达 6,000+ ops/s

#### 配置优化

1. **内存配置**
   - 根据数据量合理设置 `memory.max.entries`
   - 选择合适的淘汰策略（推荐 LRU）

2. **WAL配置**
   - 性能优先：使用 ASYNC 或 BATCH 策略
   - 数据安全优先：使用 SYNC 策略（性能较差）
   - 推荐配置：ASYNC 刷盘 + 100ms 刷盘间隔

3. **TTL配置**
   - 通用场景：使用 HYBRID 策略（推荐）
   - 读多写少：使用 LAZY 策略
   - 写多读少：使用 PERIODIC 策略

#### 监控指标

建议监控以下指标：
- 内存使用率
- WAL 文件大小
- 磁盘 I/O
- GC 频率
- API 响应时间

#### 进一步测试

建议进行以下测试：
- 并发测试：测试多线程下的性能
- 大数据量测试：测试内存淘汰的影响
- 长时间稳定性测试：测试性能是否稳定

### 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

### 📝 更新日志

#### v1.0.0 (2026-04-10)
- ✅ 完成模块化架构重构
- ✅ 实现HTTP服务器和REST API
- ✅ 开发Web管理界面（Dashboard）
- ✅ 制作跨平台发布包
- ✅ 提供完整的启动脚本和配置文件

### 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

### 🙏 致谢

感谢所有贡献者的付出！

### 📞 联系方式

- 项目地址: [GitHub](https://github.com/your-username/moon-kv)
- 问题反馈: [Issues](https://github.com/your-username/moon-kv/issues)

---

## English

### 📖 Introduction

MOON-KV is a lightweight key-value storage database based on Java, supporting WAL persistence, TTL expiration mechanism, and memory management. The project adopts a modular architecture design, providing complete HTTP REST API and Web management interface, suitable for small and medium-sized applications, embedded scenarios, and learning research.

**Lightweight Score**: ⭐⭐⭐⭐⭐ (9.2/10)

### ✨ Key Features

#### Storage Engine
- 🔹 **KV Storage** - High-performance key-value storage based on ConcurrentHashMap
- 🔹 **WAL Persistence** - Support SYNC/ASYNC/BATCH/CRON flush strategies
- 🔹 **TTL Expiration** - Support LAZY/PERIODIC/HYBRID expiration strategies
- 🔹 **Memory Management** - Support LRU/LFU/FIFO eviction strategies

#### Service Features
- 🔹 **HTTP Server** - High-performance HTTP service based on Netty
- 🔹 **REST API** - 12 RESTful API endpoints
- 🔹 **Web Dashboard** - Modern web management interface
- 🔹 **Health Check** - Comprehensive health check mechanism

### 🚀 Quick Start

#### Requirements
- Java 17 or higher
- Maven 3.6+ (only for building)

#### Using Release Package (Recommended)

```bash
# Download and extract
tar -xzf moon-kv-server-1.0.0-dist.tar.gz
cd moon-kv-server-1.0.0

# Start server
./bin/start.sh

# Access Dashboard
# Open browser: http://localhost:4070
```

#### Build from Source

```bash
git clone https://github.com/your-username/moon-kv.git
cd moon-kv
mvn clean package
java -jar moon-kv-server/target/moon-kv-server-1.0.0.jar
```

### 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<div align="center">

**Made with ❤️ by MOON-KV Team**

</div>
