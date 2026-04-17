# ReMe 服务健壮性增强实现文档

## 概述

本文档详细说明了为 ReMe 长期记忆服务实现的健壮性增强功能，包括健康检查、自动降级、错误处理等机制。

## 实现的功能

### 1. reme-server 健康检查端点增强

#### 新增端点

| 端点 | 方法 | 描述 |
|------|------|------|
| `/healthz` | GET | 深度健康检查 - 检查所有关键组件 |
| `/heartbeat` | GET | 快速心跳检测 |
| `/health/check_chroma` | GET | 专门检查 Chroma 可用性 |
| `/health/check_model` | GET | 检查模型供应商和模型可用性 |
| `/health/check_endpoints` | POST | 检查核心 API 端点是否正常工作 |

#### 健康检查内容

1. **向量存储检查**
   - Chroma 向量数据库连接性测试
   - 自动检测 Chroma 不可用情况
   - 自动切换到本地存储模式

2. **模型供应商检查**
   - 验证 LLM 供应商是否为 DashScope
   - 检查 DashScope API Key 配置
   - 验证 Embedding 模型配置

3. **核心 API 端点检查**
   - 测试 `/summary_personal_memory` 端点
   - 测试 `/retrieve_personal_memory` 端点
   - 返回响应时间和错误信息

### 2. reme-server 自动降级机制

#### Chroma 不可用时的处理

```python
# service.py 中的自动降级逻辑
if not chroma_reachable and self._vector_backend_in_use == "chroma":
    try:
        await self._close_locked()
        # 修改配置为本地存储
        self._config = ReMeServerConfig(
            ...
            preferred_vector_backend="local",
            ...
        )
        await self._start_locked()
    except Exception as switch_error:
        error_message = f"切换到本地存储失败：{switch_error}"
```

#### 降级策略

1. 首选后端失败时，自动尝试本地存储
2. 降级后更新配置和状态
3. 记录降级原因和当前使用的后端

### 3. Java 端 ReMe 服务健康检查

#### TdAgentReMeService 新增方法

```java
// 健康状态记录类
public static class HealthStatus {
    private final boolean healthy;
    private final boolean remeAvailable;
    private final boolean chromaAvailable;
    private final boolean llmAvailable;
    private final boolean embeddingAvailable;
    private final String vectorBackendInUse;
    private final String errorMessage;
    private final long lastCheckTime;
}

// 健康检查方法
public Mono<HealthStatus> checkHealth()
public HealthStatus getHealthStatus()
public boolean isHealthy()
public void resetHealthCache()
```

#### 健康状态缓存

- 缓存时长：30 秒
- 避免频繁检查影响性能
- 支持手动刷新缓存

### 4. TdAgentFactory 健康检查集成

#### 创建 Agent 时的健康检查流程

```java
// 检查 ReMe 健康状态
boolean remeHealthy = reMeService.isHealthy();
if (!remeHealthy) {
    // 打印详细的错误日志（表格形式）
    log.error("ReMe 长期记忆服务不可用，将自动降级到本地记忆模式");
    log.error("健康状态详情：");
    log.error("  - 总体状态：不健康");
    log.error("  - Chroma 向量库：不可用");
    log.error("  - LLM 模型：不可用");
    // ...
    log.error("建议操作：");
    log.error("  1. 检查 Chroma Docker 容器");
    log.error("  2. 检查 DashScope API Key");
    log.error("  3. 检查 reme-server 日志");
    log.error("  4. 自动切换到本地存储模式");
}

// 只有健康时才启用长期记忆
if (properties.getReme().isEnabled() && remeHealthy) {
    builder.longTermMemory(...)
            .longTermMemoryMode(LongTermMemoryMode.BOTH);
} else if (properties.getReme().isEnabled() && !remeHealthy) {
    log.warn("ReMe 不可用，跳过长期记忆配置，使用本地记忆模式");
}
```

### 5. TdAgentMemoryCompactionHook 降级逻辑

#### Hook 构造函数更新

```java
public TdAgentMemoryCompactionHook(
        ConversationSessionContext context,
        MongoConversationMemory memory,
        TdAgentMemoryManager memoryManager,
        TdAgentReMeService reMeService) {  // 新增参数
    ...
    this.reMeService = reMeService;
}
```

#### 推理前的健康检查

```java
@Override
public <T extends HookEvent> Mono<T> onEvent(T event) {
    if (!(event instanceof PreReasoningEvent preReasoningEvent)) {
        return Mono.just(event);
    }

    // 检查 ReMe 服务是否可用
    if (!isReMeAvailable()) {
        log.debug("[MemoryCompactionHook] ReMe 不可用，跳过记忆压缩逻辑");
        // 仍然注入可能存在的本地摘要
        preReasoningEvent.setInputMessages(
                memoryManager.injectCompressedSummary(...));
        return Mono.just(event);
    }

    // ReMe 可用时正常执行压缩逻辑
    memoryManager.maybeCompact(context, memory, ...);
    ...
}
```

### 6. 前端错误处理增强

#### useAgentConsole.ts 错误识别

```typescript
// 检查是否是最大迭代次数错误且 ReMe 不可用
const isMaxItersError = errorMessage.includes("最大执行次数") || 
                        errorMessage.includes("max iterations");

const isRemeUnavailable = errorMessage.includes("reme 不可用") ||
                          errorMessage.includes("reme unavailable");

// 显示友好提示
if (isMaxItersError || isRemeUnavailable) {
  const friendlyMessage = "已达到最大执行次数，ReMe 长期记忆服务不可用，请开启新对话。";
  // 显示警告而不是错误
  messageApi.warning(friendlyMessage);
}
```

## 使用指南

### 健康检查 API 使用

#### 1. 全面健康检查

```bash
curl http://localhost:8085/healthz
```

响应示例：
```json
{
  "ok": true,
  "status": "healthy",
  "details": {
    "vector_store": {
      "ready": true,
      "backend_in_use": "chroma",
      "preferred_backend": "chroma"
    },
    "llm": {
      "ready": true,
      "backend": "dashscope",
      "model_name": "qwen-plus",
      "is_dashscope": true
    },
    "embedding": {
      "ready": true,
      "backend": "dashscope",
      "model_name": "text-embedding-v3",
      "is_dashscope": true
    }
  }
}
```

#### 2. 单独检查 Chroma

```bash
curl http://localhost:8085/health/check_chroma
```

#### 3. 单独检查模型

```bash
curl http://localhost:8085/health/check_model
```

### 配置说明

#### reme-server 环境变量

```bash
# .env 配置示例

# 向量后端 (chroma 或 local)
REME_VECTOR_BACKEND=chroma

# Chroma 配置
REME_CHROMA_HOST=127.0.0.1
REME_CHROMA_PORT=8000
REME_CHROMA_COLLECTION=reme

# LLM 配置（使用 DashScope）
REME_LLM_BACKEND=dashscope
REME_LLM_MODEL_NAME=qwen-plus

# Embedding 配置
REME_EMBEDDING_BACKEND=dashscope
REME_EMBEDDING_MODEL_NAME=text-embedding-v3
REME_EMBEDDING_DIMENSIONS=1536

# DashScope API Key（必须）
DASHSCOPE_API_KEY=your-api-key-here
```

#### Java 端配置

```yaml
# application-agentic.yaml
tdagent:
  reme:
    enabled: true
    base-url: http://localhost:8085
    timeout-seconds: 60
    top-k: 5
```

## 故障排查

### 常见问题

#### 1. Chroma 不可用

**症状**：健康检查显示 Chroma unreachable

**解决方案**：
```bash
# 检查 Chroma 容器状态
docker ps | grep chroma

# 重启 Chroma
docker-compose restart chroma

# 查看 Chroma 日志
docker-compose logs chroma
```

#### 2. DashScope API Key 未配置

**症状**：LLM/Embedding 检查失败，显示"非 DashScope 供应商"或"API Key 未配置"

**解决方案**：
```bash
# 设置环境变量
export DASHSCOPE_API_KEY=your-api-key-here

# 或在 .env 文件中配置
DASHSCOPE_API_KEY=your-api-key-here
```

#### 3. reme-server 启动失败

**症状**：Java 端健康检查超时或连接失败

**解决方案**：
```bash
# 检查 reme-server 进程
ps aux | grep reme

# 查看 reme-server 日志
tail -f reme-server/logs/*.log

# 重启 reme-server
cd reme-server
poetry run uvicorn src.app.main:app --reload
```

### 降级模式验证

1. **手动停止 Chroma**：
   ```bash
   docker stop chroma
   ```

2. **观察日志**：
   - reme-server 应自动切换到 local 后端
   - Java 端应显示降级提示但继续运行
   - 前端应能正常对话（无长期记忆）

3. **恢复 Chroma**：
   ```bash
   docker start chroma
   ```
   - 重启 reme-server 以重新使用 Chroma

## 架构说明

### 组件关系

```
┌─────────────────────────────────────────────────────────────┐
│                      Frontend (Console)                      │
│  - 错误处理：识别 ReMe 不可用错误                            │
│  - 友好提示：显示降级消息                                    │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ HTTP/SSE
                     │
┌────────────────────▼────────────────────────────────────────┐
│              Java Agent Engine                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ TdAgentFactory                                        │   │
│  │   - 创建 Agent 时检查 ReMe 健康状态                     │   │
│  │   - 打印详细错误日志（表格形式）                       │   │
│  │   - 根据健康状态决定是否启用长期记忆                   │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ TdAgentMemoryCompactionHook                           │   │
│  │   - 推理前检查 ReMe 可用性                             │   │
│  │   - 不可用时跳过压缩逻辑                              │   │
│  │   - 使用本地摘要继续执行                              │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ TdAgentReMeService                                    │   │
│  │   - 健康检查（缓存 30 秒）                             │   │
│  │   - 状态管理                                          │   │
│  └──────────────────────────────────────────────────────┘   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ HTTP API
                     │
┌────────────────────▼────────────────────────────────────────┐
│                    reme-server (Python)                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Health Check Endpoints                                │   │
│  │   - /healthz (全面检查)                               │   │
│  │   - /health/check_chroma                              │   │
│  │   - /health/check_model                               │   │
│  │   - /health/check_endpoints                           │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ Auto Degrade Logic                                    │   │
│  │   - Chroma 不可用时自动切换 local                      │   │
│  │   - 配置热更新                                        │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ ReMe Core                                             │   │
│  │   - 记忆存储/检索                                     │   │
│  │   - 会话压缩                                          │   │
│  └──────────────────────────────────────────────────────┘   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     │ HTTP/REST
                     │
┌────────────────────▼────────────────────────────────────────┐
│                  External Services                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Chroma    │  │  DashScope  │  │  Local Storage      │  │
│  │  (Vector)   │  │   (LLM)     │  │  (Fallback)         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 健康检查流程

```
1. Java 端启动
   ↓
2. TdAgentReMeService 异步检查健康
   ↓
3. 调用 reme-server /healthz 端点
   ↓
4. reme-server 执行检查：
   - Chroma 连接性
   - DashScope 配置
   - 核心 API 可用性
   ↓
5. 返回健康状态
   ↓
6. Java 端缓存状态（30 秒）
   ↓
7. 创建 Agent 时检查缓存状态
   ↓
8. 根据健康状态决定是否启用长期记忆
```

## 总结

本次实现为 ReMe 长期记忆服务提供了完整的健壮性增强：

1. **多层健康检查**：从基础设施（Chroma）到模型（DashScope）到 API 端点的全方位检查
2. **自动降级机制**：Chroma 不可用时自动切换到本地存储
3. **友好的错误提示**：Java 端详细日志 + 前端友好提示
4. **无感知降级**：ReMe 不可用时自动跳过长期记忆逻辑，不阻塞主流程
5. **状态缓存优化**：避免频繁健康检查影响性能

通过这些机制，确保了即使 ReMe 服务不可用，整个系统仍然可以继续运行，只是缺少长期记忆功能，而不会导致流程卡死或用户体验下降。
