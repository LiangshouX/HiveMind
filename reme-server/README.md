# ReMe MCP Server

ReMe (Retrieval-enhanced Memory) 长期记忆服务，通过 MCP SSE 协议与 HiveMind Java 后端通信。

> **推荐使用 Docker Compose 一键部署整个 HiveMind 平台，详见 [部署指南](../docs/ci-deploy/README.md)。**

## 架构

```
HiveMind (Java)  ──MCP SSE──>  ReMe Server (Python)  ──>  本地文件存储
   :8080                          :8002                     /workspace/
```

## 快速开始（本地开发）

### 1. 环境准备

```bash
# 创建虚拟环境（推荐 Python 3.11）
python -m venv .venv
source .venv/bin/activate  # Linux/macOS
# .venv\Scripts\activate   # Windows

# 安装依赖
cd reme-server
pip install -r requirements.txt
```

### 2. 配置

```bash
cp .env.example .env
```

编辑 `.env`，填入 LLM API Key：

```bash
# ----- LLM 配置（必填）-----
LLM_API_KEY=sk-xxxxxxxxxxxxxxxx
LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1

# ----- Embedding 配置（可选，与 LLM 相同时可省略）-----
EMBEDDING_API_KEY=sk-xxxxxxxxxxxxxxxx
EMBEDDING_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
```

### 3. 启动 MCP 服务器

```bash
# Windows
scripts\start_mcp.bat

# Linux/macOS
./scripts/start_mcp.sh
```

### 4. 验证

```bash
# 测试 SSE 端点（应返回 SSE 流）
curl http://localhost:8002/sse
```

## Docker 部署（独立）

```bash
cd reme-server
docker build -t hivemind-reme:latest -f docker/Dockerfile .
docker run -d --name hivemind-reme -p 8002:8002 \
  -e LLM_API_KEY=sk-xxx \
  -e LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1 \
  hivemind-reme:latest
```

## 配置说明

### 环境变量

| 变量名                    | 说明                | 默认值                 | 必填 |
|------------------------|-------------------|---------------------|----|
| `LLM_API_KEY`          | LLM API Key       | -                   | ✅  |
| `LLM_BASE_URL`         | LLM API 地址        | -                   | ✅  |
| `LLM_MODEL_NAME`       | LLM 模型名           | `qwen-max`          | 否  |
| `EMBEDDING_API_KEY`    | Embedding API Key | -                   | 否  |
| `EMBEDDING_BASE_URL`   | Embedding API 地址  | DashScope           | 否  |
| `EMBEDDING_MODEL_NAME` | Embedding 模型名     | `text-embedding-v4` | 否  |
| `REME_API_KEY`         | 服务访问密钥            | -                   | 否  |

### 阿里云百炼（DashScope）配置

| 模型                  | 用途        | 推荐场景        |
|---------------------|-----------|-------------|
| `qwen-turbo`        | LLM       | 简单任务、快速响应   |
| `qwen-plus`         | LLM       | **推荐**，性能均衡 |
| `qwen-max`          | LLM       | 复杂推理、代码生成   |
| `text-embedding-v3` | Embedding | **推荐**，效果最好 |

获取 API Key：[阿里云百炼控制台](https://dashscope.console.aliyun.com/)

## HiveMind 集成

HiveMind 通过 MCP SSE 协议连接 ReMe，配置在 `application-agentic.yaml`：

```yaml
tdagent:
  reme:
    enabled: true
    base-url: http://localhost:8002
    timeout-seconds: 60
    top-k: 5
    mcp:
      enabled: true
      transport: sse
      sse:
        url: http://localhost:8002/sse
```

启动后，Agent 可使用以下 MCP 工具操作记忆：

| 工具       | 用途   | 示例参数                                     |
|----------|------|------------------------------------------|
| `search` | 搜索记忆 | `query`, `limit`                         |
| `read`   | 读取文件 | `path`                                   |
| `write`  | 写入文件 | `path`, `name`, `description`, `content` |
| `edit`   | 编辑文件 | `path`, `old`, `new`                     |
| `list`   | 列出文件 | `path`, `recursive`                      |

## 目录结构

```
reme-server/
├── .env                    # 环境配置（从 .env.example 复制，不提交）
├── .env.example            # 配置模板
├── .dockerignore           # Docker 构建排除规则
├── requirements.txt        # Python 依赖
├── scripts/
│   ├── start_mcp.sh        # Linux MCP 启动脚本
│   └── start_mcp.bat       # Windows MCP 启动脚本
├── docker/
│   └── Dockerfile          # Docker 构建文件
└── README.md               # 本文档
```

## 常见问题

### MCP 初始化失败

```
ReMe: MCP initialization failed, falling back to HTTP mode
```

检查 ReMe MCP 服务器是否运行：`curl http://localhost:8002/sse`

### LLM API 错误

```
dashscope.APIError: Invalid API key
```

检查 `LLM_API_KEY` 是否正确，账号是否有余额。

### 端口被占用

```
port 8002 occupied
```

```bash
# 使用其他端口启动
reme start service.port=8085 service.backend=mcp mcp.transport=sse
```

## 相关文档

- [HiveMind 部署指南](../docs/ci-deploy/README.md) - 完整部署文档
- [HiveMind CLAUDE.md](../CLAUDE.md) - 项目整体文档
- [.env.example](.env.example) - 完整配置模板
