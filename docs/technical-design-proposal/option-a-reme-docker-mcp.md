# 方案 A：ReMe 独立 Docker 服务 + MCP 协议集成

> **文档版本**: 1.0
> **创建日期**: 2026-07-12
> **状态**: 设计方案

---

## 1. 方案概述

将 ReMe 部署为独立的 Docker 容器服务，通过 MCP SSE 协议与 HiveMind 通信。ReMe 的文件系统、索引、记忆流水线全部保留在容器内部，HiveMind 通过 MCP 工具调用实现所有记忆操作。

```
┌─────────────────────────────────────────────────────────┐
│  HiveMind (Java, Docker/Host)                           │
│                                                         │
│  TdAgentReMeService                                     │
│    ├── McpReMeClient (新增, MCP SSE)                    │
│    │     └── callTool("search", ...)                    │
│    │     └── callTool("auto_memory", ...)               │
│    │     └── callTool("read", ...)                      │
│    │     └── callTool("write", ...)                     │
│    │     └── callTool("edit", ...)                      │
│    └── ReMeClient (保留, HTTP fallback)                 │
│                                                         │
│  前端管理界面                                             │
│    └── 记忆浏览/编辑 API (通过 McpReMeClient)             │
└─────────────────────────────────────────────────────────┘
         │ MCP SSE (端口 8002) / HTTP fallback
         ▼
┌─────────────────────────────────────────────────────────┐
│  reme-server (Docker Container)                         │
│                                                         │
│  MCPService (FastMCP, SSE transport)                    │
│    ├── 25 个 MCP Tool (search, read, write, edit, ...)  │
│    └── ChannelSink (workspace 变更通知)                   │
│                                                         │
│  Application                                            │
│    ├── Jobs: auto_memory, auto_dream, search, ...       │
│    ├── Components:                                      │
│    │   ├── LocalFileStore                               │
│    │   │   ├── BM25Index (keyword search)               │
│    │   │   ├── LocalFileGraph (wikilink graph)          │
│    │   │   └── LocalEmbeddingStore (可选, vector search) │
│    │   ├── AgentWrapper (LLM 调用)                      │
│    │   └── FileChunker (Markdown 分块)                   │
│    └── Workspace: /workspace/                           │
│         ├── session/   (原始对话)                        │
│         ├── daily/     (每日记忆)                        │
│         ├── digest/    (长期记忆)                        │
│         └── metadata/  (索引状态)                        │
│                                                         │
│  Volume: reme-workspace (持久化)                         │
└─────────────────────────────────────────────────────────┘
```

---

## 2. 详细架构设计

### 2.1 通信协议

HiveMind ↔ ReMe 之间使用 MCP SSE 协议：

| 层 | 协议 | 说明 |
|----|------|------|
| 传输层 | HTTP SSE | 端点 `/sse/`，长连接 |
| 协议层 | MCP (Model Context Protocol) | JSON-RPC 2.0 消息 |
| 应用层 | Tool Call / Tool Result | ReMe 的 Job 注册为 MCP Tool |

### 2.2 Java 侧新增组件

#### McpReMeClient.java

```java
package com.liangshou.agentic.agents.memory.reme;

import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpSyncClientWrapper;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * ReMe MCP 客户端 - 通过 MCP SSE 协议与 ReMe 服务通信。
 *
 * <p>封装 AgentScope-Java 的 McpSyncClientWrapper，提供类型安全的工具调用接口。
 * 支持自动初始化、错误降级和优雅关闭。</p>
 */
public class McpReMeClient {

    private static final Logger log = LoggerFactory.getLogger(McpReMeClient.class);

    private final String sseUrl;
    private final Duration timeout;
    private McpSyncClientWrapper clientWrapper;
    private boolean initialized = false;

    public McpReMeClient(String sseUrl, int timeoutSeconds) {
        this.sseUrl = sseUrl;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    /**
     * 初始化 MCP 连接。应在应用启动时调用。
     *
     * @return 是否初始化成功
     */
    public synchronized boolean initialize() {
        if (initialized) return true;
        try {
            clientWrapper = (McpSyncClientWrapper) McpClientBuilder.create("reme-memory")
                    .sseTransport(sseUrl)
                    .timeout(timeout)
                    .buildSync();
            clientWrapper.initialize().block();
            initialized = true;
            log.info("ReMe MCP client initialized: {}", sseUrl);
            return true;
        } catch (Exception e) {
            log.error("Failed to initialize ReMe MCP client", e);
            initialized = false;
            return false;
        }
    }

    /**
     * 列出 ReMe 暴露的所有 MCP 工具。
     */
    public List<McpSchema.Tool> listTools() {
        ensureInitialized();
        return clientWrapper.listTools().block();
    }

    /**
     * 调用 ReMe MCP 工具。
     *
     * @param toolName 工具名称 (search, auto_memory, read, write, edit, ...)
     * @param arguments 工具参数
     * @return 工具调用结果
     */
    public McpSchema.CallToolResult callTool(String toolName, Map<String, Object> arguments) {
        ensureInitialized();
        log.debug("Calling ReMe MCP tool: {} with args: {}", toolName, arguments);
        return clientWrapper.callTool(toolName, arguments).block();
    }

    /**
     * 便捷方法：调用工具并提取文本结果。
     */
    public String callToolText(String toolName, Map<String, Object> arguments) {
        McpSchema.CallToolResult result = callTool(toolName, arguments);
        return extractText(result);
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("ReMe MCP client not initialized");
        }
    }

    private static String extractText(McpSchema.CallToolResult result) {
        if (result.content() == null) return "";
        for (McpSchema.Content block : result.content()) {
            if (block instanceof McpSchema.TextContent tc) {
                return tc.text();
            }
        }
        return result.content().toString();
    }

    @PreDestroy
    public void close() {
        if (clientWrapper != null) {
            try {
                clientWrapper.close();
            } catch (Exception e) {
                log.warn("Error closing ReMe MCP client", e);
            }
        }
    }
}
```

#### TdAgentProperties.java 变更

```java
@Getter
@Setter
public static class ReMe {
    private boolean enabled = true;
    private String baseUrl = "http://localhost:8002";  // HTTP fallback
    private String apiKey;
    private int timeoutSeconds = 60;
    private int topK = 5;
    private MCP mcp = new MCP();  // 新增
}

@Getter
@Setter
public static class MCP {
    /** 是否启用 MCP 模式。为 false 时使用 HTTP fallback。 */
    private boolean enabled = false;

    /** MCP 传输模式：sse, stdio */
    private String transport = "sse";

    /** SSE 配置 */
    private SseConfig sse = new SseConfig();
}

@Getter
@Setter
public static class SseConfig {
    /** ReMe MCP Server 的 SSE 端点 URL */
    private String url = "http://localhost:8002/sse/";
}
```

#### TdAgentReMeService.java 变更

```java
@Service
public class TdAgentReMeService {

    private final TdAgentProperties properties;
    private final ReMeClient reMeClient;         // HTTP 客户端 (fallback)
    private final McpReMeClient mcpReMeClient;   // MCP 客户端 (首选)
    private final boolean useMcp;

    public TdAgentReMeService(TdAgentProperties properties) {
        this.properties = properties;

        // 尝试初始化 MCP 客户端
        TdAgentProperties.MCP mcpConfig = properties.getReme().getMcp();
        this.mcpReMeClient = new McpReMeClient(
                mcpConfig.getSse().getUrl(),
                properties.getReme().getTimeoutSeconds()
        );

        boolean mcpReady = mcpConfig.isEnabled() && mcpReMeClient.initialize();
        this.useMcp = mcpReady;

        if (useMcp) {
            this.reMeClient = null;
            log.info("ReMe: using MCP mode ({})", mcpConfig.getSse().getUrl());
        } else {
            this.reMeClient = new ReMeClient(
                    properties.getReme().getBaseUrl(),
                    java.time.Duration.ofSeconds(properties.getReme().getTimeoutSeconds())
            );
            log.info("ReMe: using HTTP mode ({})", properties.getReme().getBaseUrl());
        }
    }

    /**
     * 添加对话记忆。
     */
    public void add(String workspaceId, List<Msg> messages) {
        if (!properties.getReme().isEnabled()) return;
        List<ReMeMessage> remeMessages = toReMeMessages(messages);
        if (remeMessages.isEmpty()) return;

        if (useMcp) {
            Map<String, Object> args = Map.of(
                    "messages", remeMessages.stream()
                            .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                            .toList(),
                    "session_id", workspaceId
            );
            mcpReMeClient.callTool("auto_memory", args);
        } else {
            // HTTP fallback (现有逻辑)
            ReMeAddRequest request = ReMeAddRequest.builder()
                    .workspaceId(workspaceId)
                    .trajectories(List.of(ReMeTrajectory.builder().messages(remeMessages).build()))
                    .build();
            reMeClient.add(request).block();
        }
    }

    /**
     * 检索记忆。
     */
    public String retrieve(String workspaceId, String query) {
        if (!properties.getReme().isEnabled() || query == null || query.isBlank()) return "";

        if (useMcp) {
            Map<String, Object> args = Map.of(
                    "query", query,
                    "limit", properties.getReme().getTopK()
            );
            String result = mcpReMeClient.callToolText("search", args);
            return truncate(result);
        } else {
            // HTTP fallback (现有逻辑)
            ReMeSearchResponse response = reMeClient.search(
                    ReMeSearchRequest.builder()
                            .workspaceId(workspaceId)
                            .query(query)
                            .topK(properties.getReme().getTopK())
                            .build()
            ).block();
            // ... 现有处理逻辑
        }
    }

    // ... 其他方法
}
```

### 2.3 配置

#### application-agentic.yaml

```yaml
tdagent:
  reme:
    enabled: true
    base-url: ${TDAGENT_REME_BASE_URL:http://localhost:8002}
    timeout-seconds: 60
    top-k: 5
    mcp:
      enabled: ${REME_MCP_ENABLED:true}
      transport: ${REME_MCP_TRANSPORT:sse}
      sse:
        url: ${REME_MCP_SSE_URL:http://localhost:8002/sse/}
```

#### Docker Compose

```yaml
version: '3.8'

services:
  reme-server:
    image: reme-server:latest
    build:
      context: .
      dockerfile: Dockerfile
    container_name: hivemind-reme
    ports:
      - "8002:8002"
    volumes:
      - reme-workspace:/workspace
    env_file:
      - .env
    environment:
      - REME_BACKEND=mcp
      - MCP_TRANSPORT=sse
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8002/sse/"]
      interval: 30s
      timeout: 10s
      retries: 3
    restart: unless-stopped

volumes:
  reme-workspace:
    driver: local
```

### 2.4 前端记忆管理 API

HiveMind 后端新增 REST API，通过 McpReMeClient 代理 ReMe 的 MCP 工具：

```java
@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final TdAgentReMeService remeService;

    /**
     * 浏览用户的记忆文件列表。
     */
    @GetMapping("/files")
    public Result<List<String>> listFiles(
            @RequestParam String userId,
            @RequestParam(defaultValue = "") String path) {
        String workspaceId = userId;
        Map<String, Object> args = new HashMap<>();
        args.put("path", path);
        args.put("recursive", false);
        String result = remeService.callMcpTool("list", args);
        return Result.success(parseFileList(result));
    }

    /**
     * 读取记忆文件内容。
     */
    @GetMapping("/files/read")
    public Result<String> readFile(
            @RequestParam String userId,
            @RequestParam String path) {
        Map<String, Object> args = Map.of("path", path);
        String content = remeService.callMcpTool("read", args);
        return Result.success(content);
    }

    /**
     * 编辑记忆文件内容。
     */
    @PutMapping("/files/edit")
    public Result<Boolean> editFile(
            @RequestParam String userId,
            @RequestBody EditMemoryRequest request) {
        Map<String, Object> args = Map.of(
                "path", request.getPath(),
                "old", request.getOldText(),
                "new", request.getNewText()
        );
        remeService.callMcpTool("edit", args);
        return Result.success(true);
    }

    /**
     * 搜索记忆。
     */
    @GetMapping("/search")
    public Result<String> search(
            @RequestParam String userId,
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        String result = remeService.retrieve(userId, query);
        return Result.success(result);
    }
}
```

---

## 3. 文件操作生命周期

### 3.1 写入流程

```
Java 客户端                    ReMe MCP Server (Docker)
    │                              │
    │ callTool("write", {          │
    │   path, name,                │
    │   description, content       │
    │ })                           │
    │ ──── MCP SSE ─────────────> │
    │                              │
    │                              ├─ write_step.execute()
    │                              │   ├─ 构建 frontmatter YAML
    │                              │   └─ write_file_safe(path, content)
    │                              │       └─ 物理文件写入 workspace/
    │                              │
    │                              ├─ watch_changes_step 检测到文件变更
    │                              │   └─ update_index_step.execute()
    │                              │       ├─ MarkdownFileChunker.chunk(path)
    │                              │       │   ├─ 解析 frontmatter
    │                              │       │   ├─ AST 分块
    │                              │       │   └─ 提取 wikilinks
    │                              │       └─ file_store.upsert(node, chunks)
    │                              │           ├─ file_graph.upsert_nodes()
    │                              │           ├─ embedding_store (可选)
    │                              │           └─ keyword_index.add_docs()
    │                              │
    │ <── ToolResult ──────────────│
    │ (写入成功)                    │
```

### 3.2 编辑流程

```
Java 客户端                    ReMe MCP Server (Docker)
    │                              │
    │ callTool("edit", {           │
    │   path, old, new             │
    │ })                           │
    │ ──── MCP SSE ─────────────> │
    │                              │
    │                              ├─ edit_step.execute()
    │                              │   ├─ read_file_safe(path)
    │                              │   ├─ 解析 frontmatter (不动)
    │                              │   ├─ 在 body 中查找 old → 替换为 new
    │                              │   └─ write_file_safe(path, modified)
    │                              │
    │                              ├─ watcher 检测到变更 → 重建索引
    │                              │
    │ <── ToolResult ──────────────│
    │ (编辑成功)                    │
```

### 3.3 搜索流程

```
Java 客户端                    ReMe MCP Server (Docker)
    │                              │
    │ callTool("search", {         │
    │   query, limit               │
    │ })                           │
    │ ──── MCP SSE ─────────────> │
    │                              │
    │                              ├─ search_step.execute()
    │                              │   ├─ file_store.keyword_search(query)
    │                              │   │   └─ BM25Index.retrieve(query)
    │                              │   ├─ file_store.vector_search(query) (可选)
    │                              │   │   └─ cosine similarity
    │                              │   ├─ RRF 融合排序
    │                              │   ├─ expand_links() (wikilink 展开)
    │                              │   └─ 构建 Response
    │                              │
    │ <── ToolResult ──────────────│
    │ (搜索结果)                    │
```

---

## 4. 优势与弊端分析

### 4.1 优势

| 优势 | 说明 |
|------|------|
| **最小改动** | ReMe 零改动，Java 侧仅新增 McpReMeClient + 配置 |
| **成熟流水线** | 完整利用 auto_memory → daily → digest 记忆演化链路 |
| **MCP 生态** | 获得 MCP 协议的所有能力：Tool 注册、Channel 通知、权限控制 |
| **HTTP 降级** | McpReMeClient 初始化失败时自动降级到 HTTP，零风险 |
| **文件可读** | 记忆文件是 Markdown，运维可直接进入容器查看/调试 |
| **独立部署** | ReMe 可独立升级、重启，不影响 HiveMind 主服务 |
| **Channel 通知** | MCP 模式支持 workspace 变更实时推送到客户端 |

### 4.2 弊端

| 弊端 | 说明 | 缓解措施 |
|------|------|----------|
| **单机存储** | 文件系统无法水平扩展 | 当前用户量可接受；未来可迁移到方案 B |
| **逻辑隔离** | 所有用户的 chunk 在同一内存中，靠 workspace_id 区分 | MCP 工具层强制传入 userId；日志审计 |
| **无法直接查询** | 不能用 SQL 查询"某用户有多少记忆" | 通过 MCP `list` + `search` 工具间接查询 |
| **备份复杂** | 需要备份整个 workspace volume | Docker volume 快照；定期 rsync |
| **容器内文件不可直接编辑** | 用户无法像本地一样直接打开文件夹 | 通过前端 API 代理 MCP 工具实现编辑 |
| **单点故障** | ReMe 容器挂了，记忆功能完全不可用 | Docker restart policy；监控告警 |
| **StreamJob 不支持 MCP** | 流式 Job 无法通过 MCP 暴露 | 当前 HiveMind 仅使用非流式 Job |

### 4.3 适用场景

- ✅ 用户量中等（百级以内）
- ✅ 单机/少量服务器部署
- ✅ 重视记忆质量（auto_memory/auto_dream 流水线）
- ✅ 需要快速上线，最小化开发工作量
- ❌ 不适合需要严格数据隔离合规的场景
- ❌ 不适合需要水平扩展的大规模部署

---

## 5. 实施步骤

### Phase 1: Docker 化 ReMe (1-2 天)

1. 编写 Dockerfile（基于 Python 3.11 + reme-ai）
2. 编写 docker-compose.yml
3. 配置 .env（LLM、Embedding API Key）
4. 测试 MCP SSE 端点可用性

### Phase 2: Java MCP 客户端 (2-3 天)

1. 新增 McpReMeClient.java
2. 修改 TdAgentProperties.java（MCP 配置类）
3. 修改 TdAgentReMeService.java（MCP/HTTP 双模式）
4. 修改 application-agentic.yaml

### Phase 3: 前端记忆管理 API (1-2 天)

1. 新增 MemoryController.java
2. 实现文件列表、读取、编辑、搜索接口
3. 前端页面集成

### Phase 4: 测试与部署 (1-2 天)

1. 端到端测试：写入 → 检索 → 编辑 → 再检索
2. 多用户隔离测试
3. 故障降级测试（MCP 不可用时自动切 HTTP）
4. 部署文档

**总工期**: 约 5-9 天
