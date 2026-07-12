# ReMe 技术调研报告：架构、主流使用方式与集成方案

> **文档版本**: 1.0 \
> **创建日期**: 2026-07-12 \
> **状态**: 调研完成 \
> **源码版本**: `ReMe`（V4 最新版）

---

## 1. 调研背景

HiveMind 当前通过 HTTP 协议连接 ReMe 长期记忆服务，使用 `agentscope-extensions-reme` 提供的 `ReMeClient`。本次调研旨在全面理解 ReMe V4 的架构设计、主流使用方式和集成路径，为后续技术改造提供决策依据。

**核心结论**：ReMe V4 是一个 **文件优先（File-first）** 的记忆系统，其核心价值在于将对话和资源沉淀为可读、可编辑、可追溯的 Markdown 文件，并通过自动化的 session → daily → digest 流水线实现记忆的渐进式演化。**不建议将 ReMe 的存储后端替换为外部向量数据库（如 Chroma）**，因为这与 ReMe 的设计哲学根本冲突。

---

## 2. ReMe 核心设计哲学

### 2.1 Memory as File, File as Memory

ReMe 的核心思想是**记忆即文件**：

- **可读**：用户可以直接打开 workspace 目录，像读笔记一样读 daily、digest
- **可编辑**：用户和 Agent 都能用文件操作修正、补充、移动或删除记忆
- **可追溯**：digest 中的长期结论通过 `derived_from:: [[...]]` 回到 daily/session 原文
- **可迁移**：workspace 是普通目录，可被备份、同步、版本管理
- **可索引**：ReMe 从文件中解析 frontmatter、chunk、wikilink，构建检索索引和文件图谱
- **可协作**：人负责判断和修正，Agent 负责整理、链接和检索

### 2.2 为什么不适合外部向量数据库

ReMe V3 曾基于 Chroma/sqlite，但**因稳定性问题（core dump、低版本兼容性）被 V4 主动移除**。V4 的设计选择是：

| 设计选择 | 原因 |
|---------|------|
| 文件而非数据库 | 用户可直接读写、版本管理、迁移 |
| 本地 numpy 缓存而非向量数据库 | 避免外部依赖、简化部署、减少故障点 |
| BM25 + 可选 embedding | 开箱即用（BM25），embedding 是增强而非必需 |
| Markdown + wikilink 而非结构化存储 | 人和 Agent 都能理解 |

**结论**：如果需要外部向量存储，应该在 ReMe 之外构建一个独立的检索层，而不是修改 ReMe 的内部存储。

---

## 3. ReMe 架构详解

### 3.1 分层架构

```text
┌─────────────────────────────────────────────────────┐
│  CLI (reme.py)                                      │
│  parse_args → start / call_server                   │
├─────────────────────────────────────────────────────┤
│  Service (HTTP / MCP)                               │
│  将 Job 注册为 endpoint 或 tool                      │
├─────────────────────────────────────────────────────┤
│  Application (application.py)                       │
│  配置驱动的对象装配、依赖拓扑、生命周期管理              │
├─────────────────────────────────────────────────────┤
│  Job (BaseJob / StreamJob / BackgroundJob / CronJob) │
│  编排一组 Step 的执行单元                             │
├─────────────────────────────────────────────────────┤
│  Step (steps/)                                      │
│  业务原子操作：读写文件、检索、索引、自进化              │
├─────────────────────────────────────────────────────┤
│  Component (components/)                            │
│  可复用基础设施：file_store, file_graph, keyword_index │
│  embedding_store, agent_wrapper, as_llm, as_embedding│
├─────────────────────────────────────────────────────┤
│  Workspace (文件系统)                                │
│  session/ + resource/ → daily/ → digest/ + metadata/ │
└─────────────────────────────────────────────────────┘
```

### 3.2 记忆分层模型

```text
<workspace_dir>/
├── metadata/                    # 系统索引层：file_store、file_graph、keyword_index 持久状态
├── session/                     # 原始输入层：原始对话 (JSONL)
│   └── dialog/<session_id>.jsonl
├── resource/                    # 原始输入层：外部资源文件
│   └── YYYY-MM-DD/<resource>.<ext>
├── daily/                       # 浅加工层：当日事实、对话摘要、资源解读
│   ├── YYYY-MM-DD.md            # 当天索引页
│   └── YYYY-MM-DD/
│       ├── <session_id>.md      # 对话记忆卡片
│       ├── <resource_stem>.md   # 资源记忆卡片
│       └── interests.yaml       # auto_dream 产出的兴趣主题
└── digest/                      # 深加工层：可长期复用的记忆节点
    ├── personal/                # 用户偏好、长期个人事实
    ├── procedure/               # 流程、方法论、操作经验
    └── wiki/                    # 通用知识、概念、决策先例
```

### 3.3 记忆流转流水线

```text
对话消息 ──→ auto_memory ──→ session/dialog/*.jsonl (原始对话)
                         ──→ daily/YYYY-MM-DD/<session>.md (记忆卡片)
                         ──→ daily/YYYY-MM-DD.md (当天索引)

外部资源 ──→ auto_resource ──→ resource/YYYY-MM-DD/<file> (原始材料)
                           ──→ daily/YYYY-MM-DD/<resource>.md (资源卡片)

daily 记忆 ──→ auto_dream ──→ digest/personal/*.md (个人事实)
                          ──→ digest/procedure/*.md (流程经验)
                          ──→ digest/wiki/*.md (知识节点)
                          ──→ daily/<date>/interests.yaml (兴趣主题)

interests.yaml ──→ proactive ──→ 上层 Agent 决定是否主动提醒用户
```

---

## 4. 核心组件详解

### 4.1 FileStore（文件存储协调层）

`LocalFileStore` 是默认的 file_store 后端，组合三个子组件：

| 子组件 | 作用 | 默认状态 |
|--------|------|---------|
| `file_chunks` | 保存 FileChunk 文本、行号、可选 embedding | 启用 |
| `keyword_index` (BM25) | BM25 倒排索引 | 启用 |
| `file_graph` | wikilink 图谱 | 启用 |
| `embedding_store` | 向量 embedding 存储和检索 | **默认关闭** |

**关键点**：默认配置中 `embedding_store: ""`（空字符串），向量搜索默认禁用。开箱搜索主要是 **BM25 + wikilink 展开**。

### 4.2 混合搜索（Hybrid Search）

`search` Job 的搜索流程：

```text
query ──→ BM25 keyword_search ──→ candidates
query ──→ vector_search (if enabled) ──→ candidates
                │
                ▼
        RRF 融合排序 (Reciprocal Rank Fusion)
                │
                ▼
        min_score 过滤 → 截断到 limit → expand_links (wikilink 展开)
                │
                ▼
        Response (answer + metadata)
```

RRF 融合公式：
```text
fused_score = vector_weight / (60 + vector_rank)
            + keyword_weight / (60 + keyword_rank)
```

默认 `vector_weight=0.7`，启用 embedding 后语义召回权重更高。

### 4.3 Embedding Store（向量存储）

`LocalEmbeddingStore` 是唯一的 embedding_store 后端：

- **LRU 缓存**：内存中维护最近使用的 embedding（默认 10000 条）
- **磁盘持久化**：关闭时 dump 到 `metadata/` 下的 `.npz` 文件
- **批量计算**：miss 的文本通过外部 Embedding API 批量计算
- **维度校验**：严格校验 embedding 维度，不匹配的自动丢弃

**重要**：这不是一个"向量数据库"，而是一个**embedding 缓存层**。真正的向量搜索在 `LocalFileStore.vector_search()` 中通过 numpy 的 `batch_cosine_similarity` 做暴力计算。

### 4.4 Agent Wrapper（Agent 封装）

ReMe 支持两种 Agent wrapper：

| Wrapper | 用途 | 说明 |
|---------|------|------|
| `agentscope` | 通用 AgentScope agent | 用于 auto_memory、auto_dream 等需要 LLM 的步骤 |
| `claude_code` | Claude Code 集成 | 用于 Claude Code 插件的 ReAct 循环 |

Agent wrapper 提供：
- ReAct 循环（最大迭代次数可配置）
- 上下文窗口管理（trigger_ratio、reserve_ratio）
- 工具权限控制（permission_mode）

---

## 5. 服务暴露方式

### 5.1 HTTP Service

```bash
reme start                          # 默认 HTTP 模式
reme start service.port=8181        # 自定义端口
```

- 所有非 StreamJob 注册为 `POST /<job.name>`，返回 JSON Response
- StreamJob 注册为 `POST /<job.name>`，返回 SSE (text/event-stream)
- 支持 CORS

### 5.2 MCP Service

```bash
reme start backend=mcp                          # MCP 模式，默认 SSE 传输
reme start backend=mcp mcp.transport=sse         # 显式指定 SSE
reme start backend=mcp mcp.transport=stdio       # StdIO 传输
reme start backend=mcp mcp.transport=streamable-http  # Streamable HTTP
```

- 使用 `fastmcp` 库实现
- 所有非 StreamJob 自动注册为 MCP Tool
- **StreamJob 当前不支持 MCP 暴露**
- SSE 端点路径为 `/sse/`（注意尾部斜杠）
- 支持 Channel 机制（workspace 变更通知）

### 5.3 传输模式对比

| 模式 | 端点 | 适用场景 |
|------|------|---------|
| HTTP | `POST /<job>` | 传统 REST 集成、现有 Java 客户端 |
| MCP SSE | `/sse/` | MCP 生态、Claude Code、远程服务 |
| MCP StdIO | 标准输入输出 | 本地进程间调用 |
| MCP StreamableHTTP | `/mcp` | 新版 MCP 标准 |

---

## 6. 当前 HiveMind 集成方式

### 6.1 架构图

```text
┌──────────────────────────────────────────────────────────┐
│  HiveMind (Java)                                         │
│                                                          │
│  TdAgentReMeService                                      │
│    ├── ReMeClient (agentscope-extensions-reme)           │
│    │     └── HTTP POST → http://localhost:8002/<job>     │
│    └── compactSessionHistory() / add() / retrieve()      │
│                                                          │
│  TdAgentMemoryManager                                    │
│    └── 使用 TdAgentReMeService 管理记忆生命周期            │
│                                                          │
│  ContextCompressor                                       │
│    └── 使用 TdAgentReMeService 压缩对话历史                │
└──────────────────────────────────────────────────────────┘
         │ HTTP
         ▼
┌──────────────────────────────────────────────────────────┐
│  reme-server (Python)                                    │
│                                                          │
│  reme start backend=http                                 │
│    ├── Service: HttpService (FastAPI + uvicorn)          │
│    ├── Jobs: auto_memory, search, read, write, ...       │
│    ├── Components: LocalFileStore, BM25Index, ...        │
│    └── Workspace: .reme/ (session → daily → digest)      │
└──────────────────────────────────────────────────────────┘
```

### 6.2 当前使用的 Job

| Job | Java 调用方 | 用途 |
|-----|-----------|------|
| `search` | `TdAgentReMeService.retrieve()` | 语义检索长期记忆 |
| `auto_memory` | `TdAgentReMeService.add()` | 将对话消息写入 daily 记忆 |
| (内部) | `TdAgentReMeService.compactSessionHistory()` | 压缩对话历史 |

### 6.3 配置

```yaml
# application-agentic.yaml
tdagent:
  reme:
    enabled: true
    base-url: ${TDAGENT_REME_BASE_URL:http://localhost:8002}
    timeout-seconds: 60
    top-k: 5
```

---

## 7. AgentScope-Java MCP 客户端能力

### 7.1 可用类（`io.agentscope.core.tool.mcp`）

| 类 | 职责 |
|----|------|
| `McpClientBuilder` | 流式构建器，创建 MCP 客户端 |
| `McpClientWrapper` | 抽象基类：生命周期、工具缓存、callTool/listTools |
| `McpSyncClientWrapper` | 同步实现（阻塞调用在 boundedElastic 调度器上） |
| `McpAsyncClientWrapper` | 异步/响应式实现 |
| `McpTool` | 将 MCP tool 适配为 AgentScope 的 AgentTool |
| `McpContentConverter` | MCP 结果 → AgentScope ToolResultBlock 转换 |

### 7.2 支持的传输方式

| 传输 | 方法 | 说明 |
|------|------|------|
| StdIO | `.stdioTransport(command, args...)` | 本地进程 |
| SSE | `.sseTransport(url)` | HTTP Server-Sent Events |
| Streamable HTTP | `.streamableHttpTransport(url)` | 新版 MCP 标准 |

### 7.3 使用示例

```java
// SSE 连接
McpClientWrapper client = McpClientBuilder.create("reme-memory")
    .sseTransport("http://localhost:8002/sse")
    .timeout(Duration.ofSeconds(60))
    .buildSync();

client.initialize().block();

// 列出工具
List<McpSchema.Tool> tools = client.listTools().block();

// 调用工具
McpSchema.CallToolResult result = client.callTool(
    "search",
    Map.of("query", "用户偏好", "limit", 5)
).block();

// 清理
client.close();
```

### 7.4 关键发现

- **`agentscope-extensions-reme` 不包含 MCP 能力**：仅提供 HTTP 客户端 `ReMeClient`
- **MCP 客户端在 `agentscope-core` 中**：需要直接使用 `McpClientBuilder`
- **HiveMind 当前无任何 MCP 客户端使用**：`SysMcpPO`/`SysMcpController` 仅为 CRUD 配置层

---

## 8. ReMe MCP Server 暴露的工具

当 ReMe 以 MCP 模式启动时，以下 Job 自动注册为 MCP Tool：

| MCP Tool Name | 对应 Job | 功能 |
|---------------|---------|------|
| `search` | search | 混合检索（BM25 + 向量 + wikilink 展开） |
| `auto_memory` | auto_memory | 对话消息 → daily 记忆卡片 |
| `auto_memory_cc` | auto_memory_cc | Claude Code session → daily 记忆 |
| `auto_resource` | auto_resource | 资源文件 → daily 资源卡片 |
| `auto_dream` | auto_dream | daily → digest 长期记忆沉淀 |
| `proactive` | proactive | 读取当日兴趣主题 |
| `read` | read | 读取 Markdown 文件 |
| `write` | write | 写入 Markdown 文件 |
| `daily_write` | daily_write | 写入每日笔记 |
| `edit` | edit | 查找替换编辑 |
| `delete` | delete | 删除文件 |
| `move` | move | 移动/重命名文件 |
| `list` | list | 列出目录内容 |
| `stat` | stat | 获取文件状态 |
| `traverse` | traverse | 沿 wikilink 图谱遍历 |
| `node_search` | node_search | digest 节点级召回 |
| `reindex` | reindex | 重建索引 |
| `frontmatter_read` | frontmatter_read | 读取 frontmatter |
| `frontmatter_update` | frontmatter_update | 更新 frontmatter |
| `frontmatter_delete` | frontmatter_delete | 删除 frontmatter 键 |
| `daily_list` | daily_list | 列出某天的笔记 |
| `daily_reindex` | daily_reindex | 重建当天索引页 |
| `version` | version | 版本信息 |
| `health_check` | health_check | 健康检查 |
| `help` | help | 列出所有 Job |

---

## 9. Claude Code 集成参考

ReMe 的 `plugins/reme/` 目录提供了 Claude Code 集成的参考实现：

### 9.1 目录结构

```text
plugins/reme/
├── hooks/          # 生命周期钩子（Stop hook 自动记录 session）
└── skills/         # reme-memory skill（SKILL.md）
```

### 9.2 集成模式

- **Stop Hook**：Claude Code 会话结束时自动调用 `auto_memory_cc` 记录 session
- **Skill**：提供 `reme-memory` skill，Agent 可以主动调用 search/read/write 等操作
- **Channel**：MCP 模式下支持 workspace 变更通知推送到客户端

---

## 10. 改造建议

### 10.1 推荐方案：MCP 集成（保留现有存储）

**不替换存储后端**，仅将通信协议从 HTTP 改为 MCP：

```text
┌─────────────────────────────────────────────────┐
│  HiveMind (Java)                                │
│                                                 │
│  TdAgentReMeService                             │
│    ├── McpReMeClient (新增)                     │
│    │     └── MCP SSE → http://localhost:8002/sse│
│    └── ReMeClient (保留, 作为 fallback)         │
└─────────────────────────────────────────────────┘
         │ MCP SSE (首选) / HTTP (降级)
         ▼
┌─────────────────────────────────────────────────┐
│  reme-server (Python)                           │
│                                                 │
│  reme start backend=mcp mcp.transport=sse       │
│    ├── Service: MCPService (FastMCP)            │
│    ├── Jobs: 同上                                │
│    └── Workspace: .reme/ (文件系统不变)           │
└─────────────────────────────────────────────────┘
```

**优势**：
- 零存储改动，不破坏 ReMe 的文件优先设计
- 获得 MCP 生态的所有能力（Tool 注册、Channel 通知等）
- HTTP fallback 保留，降级无风险
- 实现工作量小（Java 侧新增 McpReMeClient + 配置）

### 10.2 不推荐方案：Chroma 向量数据库

**原因**：
1. ReMe V4 **主动移除了 Chroma**，因为 core dump 和兼容性问题
2. ReMe 的核心价值是"记忆即文件"，外部数据库破坏了可读、可编辑、可迁移的特性
3. ReMe 的检索是 BM25 + wikilink 展开为主，向量搜索是增强功能
4. 外部向量数据库增加了部署复杂度和故障点

### 10.3 如需更强的向量检索

如果业务确实需要更强的向量检索能力（例如百万级记忆），建议：

1. **启用 ReMe 内置的 embedding_store**：将 `embedding_store: ""` 改为 `embedding_store: default`，开启 FAISS 索引
2. **使用 FaissLocalFileStore**：ReMe 已内置 FAISS 后端（`backend: faiss`），支持更高效的向量检索
3. **外部检索层**：在 ReMe 之外构建独立的向量检索服务，定期从 ReMe workspace 同步数据

---

## 11. 部署架构

### 11.1 当前部署

```text
reme-server/
├── .env                    # API Key、LLM/Embedding 配置
├── scripts/
│   ├── start_windows.bat   # Windows 启动脚本 (HTTP 模式)
│   └── start_linux.sh      # Linux 启动脚本 (HTTP 模式)
├── docker/
│   ├── Dockerfile          # (空)
│   └── docker-compose.yml  # (空)
└── requirements.txt        # reme-ai==0.4.0.9, fastapi, uvicorn
```

### 11.2 MCP 模式部署

启动命令变更：

```bash
# 当前 (HTTP)
reme backend=http

# 目标 (MCP SSE)
reme start backend=mcp mcp.transport=sse
```

环境变量不变，仍需配置：
- `LLM_API_KEY` / `LLM_BASE_URL`：LLM 服务（auto_memory、auto_dream 需要）
- `EMBEDDING_API_KEY` / `EMBEDDING_BASE_URL`：Embedding 服务（可选，启用向量搜索时需要）
- `REME_API_KEY`：服务访问密钥（可选）

### 11.3 Docker Compose（新增）

```yaml
version: '3.8'

services:
  reme-server:
    build:
      context: ..
      dockerfile: docker/Dockerfile
    container_name: hivemind-reme
    ports:
      - "8002:8002"
    env_file:
      - ../.env
    environment:
      - REME_BACKEND=mcp
      - MCP_TRANSPORT=sse
    restart: unless-stopped
```

---

## 12. 风险与注意事项

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| MCP 客户端初始化失败 | 无法使用记忆功能 | 保留 HTTP fallback，自动降级 |
| ReMe Server 未启动 | Java 应用启动超时 | 延迟初始化或启动时重试 |
| SSE 端点路径错误 | 404 错误 | 使用正确的 `/sse/` 路径（带尾部斜杠） |
| StreamJob 不支持 MCP | 流式功能不可用 | 当前 HiveMind 仅使用非流式 Job，无影响 |
| Embedding 模型不可用 | 向量搜索降级为纯 BM25 | 默认已关闭向量搜索，BM25 独立工作 |

---

## 13. 参考资料

| 资源 | 路径/URL |
|------|---------|
| ReMe 源码 | `.lib-repo/ReMe/` |
| ReMe 框架文档 | `.lib-repo/ReMe/docs/zh/framework.md` |
| Memory Search 文档 | `.lib-repo/ReMe/docs/zh/memory_search.md` |
| Auto Memory 文档 | `.lib-repo/ReMe/docs/zh/auto_memory.md` |
| Auto Dream 文档 | `.lib-repo/ReMe/docs/zh/auto_dream.md` |
| Memory as File 文档 | `.lib-repo/ReMe/docs/zh/memory_as_file.md` |
| Proactive 文档 | `.lib-repo/ReMe/docs/zh/proactive.md` |
| 当前 ReMe 集成代码 | `hivemind-agent-engine/.../agents/memory/reme/TdAgentReMeService.java` |
| 当前配置 | `hivemind-agent-engine/src/main/resources/application-agentic.yaml` |
| reme-server 部署 | `reme-server/` |
| AgentScope-Java MCP | `io.agentscope.core.tool.mcp` 包（agentscope-core-1.0.12.jar） |
