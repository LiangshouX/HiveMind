# ReMe 技术改造方案：Chroma 存储 + MCP Server

> **文档版本**: 1.0  
> **创建日期**: 2026-07-11  
> **状态**: 设计方案（未实施）

---

## 1. 背景与目标

### 1.1 当前架构问题

当前 ReMe (Retrieval-enhanced Memory) 长期记忆服务存在两个限制：

1. **存储瓶颈**：使用本地文件系统存储向量数据，不利于扩展和备份
2. **协议限制**：仅支持 HTTP REST API，无法与 MCP 生态集成

### 1.2 改造目标

1. **存储升级**：将向量存储从本地文件系统迁移到 Chroma 向量数据库
2. **协议升级**：将 ReMe Server 从 HTTP 模式改为 MCP Server 模式
3. **向后兼容**：保留 HTTP 模式作为 fallback 选项

---

## 2. 技术调研结果

### 2.1 ReMe 原生 MCP 支持

**重要发现**：ReMe (`reme-ai==0.3.1.2`) 已原生支持 MCP Server，无需编写额外代码。

启动命令：
```bash
# HTTP 模式（当前）
reme backend=http

# MCP 模式（目标）
reme backend=mcp mcp.transport=sse
```

### 2.2 MCP 传输模式

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| `sse` | HTTP Server-Sent Events | 远程服务、Web 应用 |
| `stdio` | 标准输入输出 | 本地进程间调用 |
| `streamable-http` | HTTP 流式传输 | 新版 MCP 标准 |

### 2.3 FastMCP SSE 端点

ReMe 使用 FastMCP 库实现 MCP Server，SSE 端点路径为 `/sse/`（不是根路径 `/`）。

### 2.4 Chroma 向量数据库

ReMe 已原生支持 Chroma 作为向量后端：
```bash
REME_VECTOR_BACKEND=chroma
REME_CHROMA_HOST=chroma
REME_CHROMA_PORT=8000
```

### 2.5 AgentScope-Java MCP 客户端

`agentscope-core-1.0.12.jar` 包含 MCP 客户端实现：
- `io.agentscope.core.tool.mcp.McpClientBuilder` - 构建 MCP 客户端
- `io.agentscope.core.tool.mcp.McpSyncClientWrapper` - 同步客户端
- 支持 stdio、SSE、StreamableHTTP 等传输方式

---

## 3. 实现方案

### 3.1 Phase 1: Chroma 存储配置

#### 3.1.1 更新 reme-server/.env

```bash
# 向量后端改为 chroma
REME_VECTOR_BACKEND=chroma
REME_CHROMA_HOST=chroma  # Docker 网络地址
REME_CHROMA_PORT=8000

# MCP Server 配置
REME_BACKEND=mcp
MCP_TRANSPORT=sse
```

#### 3.1.2 Docker Compose 配置

```yaml
version: '3.8'

services:
  chroma:
    image: chromadb/chroma:latest
    container_name: hivemind-chroma
    ports:
      - "8000:8000"
    volumes:
      - chroma-data:/chroma/chroma
    environment:
      - ANONYMIZED_TELEMETRY=False
    restart: unless-stopped

  reme-server:
    build:
      context: ..
      dockerfile: docker/Dockerfile
    container_name: hivemind-reme
    ports:
      - "8002:8002"
    depends_on:
      - chroma
    env_file:
      - ../.env
    environment:
      - REME_VECTOR_BACKEND=chroma
      - REME_CHROMA_HOST=chroma
      - REME_CHROMA_PORT=8000
    restart: unless-stopped

volumes:
  chroma-data:
    driver: local
```

### 3.2 Phase 2: MCP Server 配置

#### 3.2.1 启动脚本

**Windows (start_windows.bat)**：
```batch
@echo off
REM Usage: scripts\start_windows.bat [http|mcp] [sse|stdio]

set "CONDA_ENV=reme-serve"
set "BACKEND=%~1"
if "%BACKEND%"=="" set "BACKEND=mcp"

set "MCP_TRANSPORT=%~2"
if "%MCP_TRANSPORT%"=="" set "MCP_TRANSPORT=sse"

call conda activate %CONDA_ENV%

if "%BACKEND%"=="mcp" (
    reme backend=mcp mcp.transport=%MCP_TRANSPORT%
) else (
    reme backend=http
)
```

**Linux/Mac (start.sh)**：
```bash
#!/bin/bash
# Usage: scripts/start.sh [http|mcp] [sse|stdio]

CONDA_ENV="reme-serve"
BACKEND="${1:-mcp}"
MCP_TRANSPORT="${2:-sse}"

conda activate "$CONDA_ENV"

if [ "$BACKEND" = "mcp" ]; then
    reme backend=mcp mcp.transport="$MCP_TRANSPORT"
else
    reme backend=http
fi
```

### 3.3 Phase 3: Java 客户端改造

#### 3.3.1 新增 McpReMeClient.java

```java
package com.liangshou.agentic.agents.memory.reme;

import com.liangshou.agentic.common.config.TdAgentProperties;
import io.agentscope.core.tool.mcp.McpClientBuilder;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
public class McpReMeClient {

    private static final Logger log = LoggerFactory.getLogger(McpReMeClient.class);

    private final TdAgentProperties properties;
    private McpClientWrapper clientWrapper;
    private boolean initialized = false;

    public McpReMeClient(TdAgentProperties properties) {
        this.properties = properties;
        if (properties.getReme().getMcp().isEnabled()) {
            initialize();
        }
    }

    private void initialize() {
        TdAgentProperties.MCP mcpConfig = properties.getReme().getMcp();
        String transport = mcpConfig.getTransport();

        try {
            McpClientBuilder builder = McpClientBuilder.create("reme-memory");

            if ("stdio".equalsIgnoreCase(transport)) {
                TdAgentProperties.StdioConfig stdioConfig = mcpConfig.getStdio();
                builder.stdioTransport(stdioConfig.getCommand(), stdioConfig.getArgs());
            } else if ("sse".equalsIgnoreCase(transport)) {
                TdAgentProperties.SseConfig sseConfig = mcpConfig.getSse();
                builder.sseTransport(sseConfig.getUrl())
                        .timeout(Duration.ofSeconds(properties.getReme().getTimeoutSeconds()));
            }

            this.clientWrapper = builder.buildSync();
            this.clientWrapper.initialize().block();
            this.initialized = true;
            log.info("ReMe MCP client initialized successfully");

        } catch (Exception e) {
            log.error("Failed to initialize ReMe MCP client", e);
            this.initialized = false;
        }
    }

    public Map<String, Object> callTool(String toolName, Map<String, Object> arguments) {
        if (!initialized || clientWrapper == null) {
            return Map.of("success", false, "error", "MCP client not initialized");
        }

        try {
            McpSchema.CallToolResult result = clientWrapper.callTool(toolName, arguments).block();
            return parseToolResult(result);
        } catch (Exception e) {
            log.error("Failed to call MCP tool: {}", toolName, e);
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    // ... 其他方法

    public boolean isInitialized() {
        return initialized;
    }

    @PreDestroy
    void destroy() {
        if (clientWrapper != null) {
            clientWrapper.close();
        }
    }
}
```

#### 3.3.2 更新 TdAgentProperties.java

```java
@Getter
@Setter
public static class ReMe {
    private boolean enabled = true;
    private String baseUrl = "http://localhost:8085";
    private String apiKey;
    private int timeoutSeconds = 60;
    private int topK = 5;
    private MCP mcp = new MCP();
}

@Getter
@Setter
public static class MCP {
    private boolean enabled = false;
    private String transport = "stdio";
    private StdioConfig stdio = new StdioConfig();
    private SseConfig sse = new SseConfig();
}

@Getter
@Setter
public static class StdioConfig {
    private String command = "reme";
    private String[] args = {"backend=mcp", "mcp.transport=stdio"};
    private String workingDirectory = "./reme-server";
}

@Getter
@Setter
public static class SseConfig {
    private String url = "http://localhost:8002/sse/";
}
```

#### 3.3.3 更新 TdAgentReMeService.java

```java
@Service
public class TdAgentReMeService {

    private final TdAgentProperties properties;
    private final ReMeClient reMeClient;  // HTTP 客户端
    private final McpReMeClient mcpReMeClient;  // MCP 客户端
    private final boolean useMcp;

    public TdAgentReMeService(TdAgentProperties properties, McpReMeClient mcpReMeClient) {
        this.properties = properties;
        this.mcpReMeClient = mcpReMeClient;
        this.useMcp = properties.getReme().getMcp().isEnabled() && mcpReMeClient.isInitialized();

        if (useMcp) {
            this.reMeClient = null;
        } else {
            this.reMeClient = new ReMeClient(
                    properties.getReme().getBaseUrl(),
                    java.time.Duration.ofSeconds(properties.getReme().getTimeoutSeconds())
            );
        }
    }

    public void add(String workspaceId, List<Msg> messages) {
        // ... 根据 useMcp 选择调用方式
    }

    public String retrieve(String workspaceId, String query) {
        // ... 根据 useMcp 选择调用方式
    }
}
```

#### 3.3.4 更新 application-agentic.yaml

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
      stdio:
        command: reme
        args: ["backend=mcp", "mcp.transport=stdio"]
        working-directory: ${REME_SERVER_DIR:./reme-server}
      sse:
        url: ${REME_MCP_SSE_URL:http://localhost:8002/sse/}
```

---

## 4. 文件变更清单

### 4.1 新增文件

| 文件 | 说明 |
|------|------|
| `hivemind-agent-engine/.../memory/reme/McpReMeClient.java` | MCP 客户端封装 |
| `reme-server/scripts/start.sh` | Linux/Mac 启动脚本 |

### 4.2 修改文件

| 文件 | 说明 |
|------|------|
| `reme-server/.env.example` | 添加 Chroma 和 MCP 配置项 |
| `reme-server/docker/docker-compose.yml` | 添加 Chroma 服务 |
| `reme-server/scripts/start_windows.bat` | 更新启动脚本支持 MCP |
| `hivemind-agent-engine/.../config/TdAgentProperties.java` | 添加 MCP 配置类 |
| `hivemind-agent-engine/.../memory/reme/TdAgentReMeService.java` | 支持双模式（MCP/HTTP） |
| `hivemind-agent-engine/src/main/resources/application-agentic.yaml` | 添加 MCP 配置 |

---

## 5. 部署步骤

### 5.1 环境准备

```bash
# 1. 安装 Chroma Docker
docker pull chromadb/chroma:latest

# 2. 配置 reme-server/.env
cp reme-server/.env.example reme-server/.env
# 编辑 .env，填入 API Key 和 Chroma 配置

# 3. 确保 conda 环境已创建
conda create -n reme-serve python=3.10
conda activate reme-serve
pip install reme-ai==0.4.0.9 chromadb
```

### 5.2 启动服务

```bash
# 1. 启动 Chroma
docker run -d -p 8000:8000 --name hivemind-chroma chromadb/chroma

# 2. 启动 ReMe MCP Server
cd reme-server/scripts
.\start_windows.bat mcp sse

# 3. 启动 Java 应用（需要设置环境变量）
set REME_MCP_ENABLED=true
set REME_MCP_TRANSPORT=sse
set REME_MCP_SSE_URL=http://localhost:8002/sse/
mvn spring-boot:run -pl hivemind-launcher
```

### 5.3 验证测试

```bash
# 1. 检查 Chroma 是否运行
curl http://localhost:8000/api/v1/heartbeat

# 2. 检查 ReMe MCP Server 是否运行
curl http://localhost:8002/sse/
# 应返回 SSE 流响应

# 3. 测试 Java 客户端连接
# 通过 API 调用记忆功能
```

---

## 6. 风险与注意事项

### 6.1 技术风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| MCP 客户端初始化失败 | 回退到 HTTP 模式 | 确保 MCP Server 先于 Java 应用启动 |
| Chroma 连接失败 | 无法存储向量 | 配置健康检查和重试机制 |
| SSE 端点路径错误 | 404 错误 | 使用正确的 `/sse/` 路径 |

### 6.2 部署注意事项

1. **启动顺序**：先启动 Chroma，再启动 ReMe MCP Server，最后启动 Java 应用
2. **环境变量**：确保 `REME_MCP_ENABLED=true` 已设置
3. **端口配置**：ReMe 默认使用 8002 端口，确保无冲突
4. **Conda 环境**：环境名是 `reme-serve`，不是 `reme-server`

### 6.3 向后兼容

- 保留 HTTP 模式作为 fallback
- 通过配置开关控制 MCP/HTTP 模式切换
- 不影响现有的记忆数据

---

## 7. 后续工作

1. **性能测试**：测试 Chroma 存储的读写性能
2. **数据迁移**：将现有本地向量数据迁移到 Chroma
3. **监控告警**：添加 MCP 连接状态监控
4. **文档完善**：更新部署文档和运维手册

---

## 8. 参考资料

- [ReMe GitHub](https://github.com/agentscope-ai/ReMe)
- [FastMCP 文档](https://github.com/jlowin/fastmcp)
- [ChromaDB 文档](https://docs.trychroma.com/)
- [AgentScope-Java MCP](https://github.com/agentscope-ai/agentscope-java)
- [MCP 协议规范](https://modelcontextprotocol.io/)
