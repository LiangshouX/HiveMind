# ReMe MCP Server

ReMe (Retrieval-enhanced Memory) 长期记忆服务，通过 MCP SSE 协议与 HiveMind Java 后端通信。

## 架构

```
HiveMind (Java)  ──MCP SSE──>  ReMe Server (Python)  ──>  本地文件存储
   :8080                          :2333                     .reme/workspace/
```

## 快速开始

### 1. 环境准备

```bash
# 创建 conda 环境（首次）
conda create -n reme-serve python=3.11 -y
conda activate reme-serve

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
FLOW_LLM_API_KEY=sk-xxxxxxxxxxxxxxxx
FLOW_LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
FLOW_LLM_MODEL_NAME=qwen-plus

# ----- Embedding 配置（可选，与 LLM 相同时可省略）-----
FLOW_EMBEDDING_API_KEY=sk-xxxxxxxxxxxxxxxx
FLOW_EMBEDDING_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
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
curl http://localhost:2333/sse
```

## 配置说明

### 环境变量

| 变量名                       | 说明                | 默认值         | 必填 |
|---------------------------|-------------------|-------------|----|
| `FLOW_LLM_API_KEY`        | LLM API Key       | -           | ✅  |
| `FLOW_LLM_BASE_URL`       | LLM API 地址        | DashScope   | 否  |
| `FLOW_LLM_MODEL_NAME`     | LLM 模型名           | `qwen-plus` | 否  |
| `FLOW_EMBEDDING_API_KEY`  | Embedding API Key | 同 LLM       | 否  |
| `FLOW_EMBEDDING_BASE_URL` | Embedding API 地址  | 同 LLM       | 否  |
| `HTTP_PORT`               | 服务端口              | `2333`      | 否  |
| `LOG_LEVEL`               | 日志级别              | `INFO`      | 否  |
| `REME_API_KEY`            | 访问密钥（可选）          | -           | 否  |

### 阿里云百炼（DashScope）配置

| 模型                  | 用途        | 推荐场景        |
|---------------------|-----------|-------------|
| `qwen-turbo`        | LLM       | 简单任务、快速响应   |
| `qwen-plus`         | LLM       | **推荐**，性能均衡 |
| `qwen-max`          | LLM       | 复杂推理、代码生成   |
| `text-embedding-v3` | Embedding | **推荐**，效果最好 |

获取 API Key：[阿里云百炼控制台](https://dashscope.console.aliyun.com/)

### 认证配置（可选）

生产环境建议启用认证：

```bash
# reme-server/.env
REME_API_KEY=your-secret-key-32chars
```

```yaml
# HiveMind application-agentic.yaml
tdagent:
  reme:
    api-key: your-secret-key-32chars
```

本地开发可不设置 `REME_API_KEY`，此时认证自动禁用。

## HiveMind 集成

HiveMind 通过 MCP SSE 协议连接 ReMe，配置在 `application-agentic.yaml`：

```yaml
tdagent:
  reme:
    enabled: true
    base-url: http://localhost:2333
    timeout-seconds: 60
    top-k: 5
    mcp:
      enabled: true
      transport: sse
      sse:
        url: http://localhost:2333/sse
```

启动后，Agent 可使用以下 MCP 工具操作记忆：

| 工具       | 用途   | 示例参数                                     |
|----------|------|------------------------------------------|
| `search` | 搜索记忆 | `query`, `limit`                         |
| `read`   | 读取文件 | `path`                                   |
| `write`  | 写入文件 | `path`, `name`, `description`, `content` |
| `edit`   | 编辑文件 | `path`, `old`, `new`                     |
| `list`   | 列出文件 | `path`, `recursive`                      |

## Docker 部署

```bash
cd reme-server/docker
docker-compose up -d
```

## 目录结构

```
reme-server/
├── .env                    # 环境配置（从 .env.example 复制）
├── .env.example            # 配置模板
├── requirements.txt        # Python 依赖
├── scripts/
│   ├── start_mcp.sh        # Linux MCP 启动脚本
│   └── start_mcp.bat       # Windows MCP 启动脚本
├── docker/
│   ├── Dockerfile
│   └── docker-compose.yml
└── .reme/                  # 数据目录（自动创建）
    ├── workspace/          # 记忆文件存储
    │   ├── daily/          # 每日笔记
    │   ├── digest/         # 长期记忆
    │   ├── session/        # 会话记录
    │   └── metadata/       # 索引元数据
    └── vectors/            # 向量索引
```

## 常见问题

### MCP 初始化失败

```
ReMe: MCP initialization failed, falling back to HTTP mode
```

检查 ReMe MCP 服务器是否运行：`curl http://localhost:2333/sse`

### LLM API 错误

```
dashscope.APIError: Invalid API key
```

检查 `FLOW_LLM_API_KEY` 是否正确，账号是否有余额。

### 端口被占用

```
port 2333 occupied
```

```bash
# 使用其他端口启动
reme start service.port=8002 service.backend=mcp mcp.transport=sse
```

## 相关文档

- [HiveMind CLAUDE.md](../CLAUDE.md) - 项目整体文档
- [.env.example](.env.example) - 完整配置模板
