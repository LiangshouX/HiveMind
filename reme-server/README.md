# ReMe Server - 长期记忆服务

基于 [ReMe (Retrieval-enhanced Memory)](https://gitee.com/chenfei6095/ReMe) 构建的长期记忆服务，提供与官方 API 完全兼容的端点。

## 功能特性

- ✅ **官方 API 兼容**: 完全兼容 Java AgentScope-ReMe 客户端
- ✅ **双重端点支持**: 同时支持官方 API 和自定义 API
- ✅ **API Key 认证**: 可选的认证机制，保护服务安全
- ✅ **CORS 支持**: 支持跨域请求
- ✅ **健康检查**: 提供 `/healthz` 和 `/heartbeat` 端点
- ✅ **向量后端选择**: 支持 Chroma 或本地向量存储

## API 端点

### 官方 API（Java 客户端使用）

| 端点                          | 方法   | 描述        |
|-----------------------------|------|-----------|
| `/summary_personal_memory`  | POST | 添加记忆并生成摘要 |
| `/retrieve_personal_memory` | POST | 检索记忆      |

### 自定义 API（保留向后兼容）

| 端点                        | 方法   | 描述     |
|---------------------------|------|--------|
| `/add_memory`             | POST | 添加记忆   |
| `/summarize_memory`       | POST | 摘要记忆   |
| `/retrieve_memory`        | POST | 检索记忆   |
| `/get_memory/{memory_id}` | GET  | 获取单条记忆 |
| `/update_memory`          | POST | 更新记忆   |
| `/list_memory`            | POST | 列出记忆   |
| `/delete_memory`          | POST | 删除记忆   |
| `/delete_all`             | POST | 删除所有记忆 |

### 健康检查

| 端点           | 方法  | 描述    |
|--------------|-----|-------|
| `/healthz`   | GET | 健康检查  |
| `/heartbeat` | GET | 心跳检测  |
| `/`          | GET | 服务器信息 |

## 快速开始

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 配置环境变量

复制 `.env.example` 到 `.env` 并根据需要修改：

```bash
cp .env.example .env
```

**默认配置（阿里云百炼）**：

```bash
# API Key 认证（可选）
REME_API_KEY=your-secret-key

# LLM 配置 - 阿里云百炼
REME_LLM_BACKEND=dashscope
REME_LLM_MODEL_NAME=qwen-plus

# Embedding 配置 - 阿里云百炼
REME_EMBEDDING_BACKEND=dashscope
REME_EMBEDDING_MODEL_NAME=text-embedding-v3

# 向量后端（chroma 或 local）
REME_VECTOR_BACKEND=local

# 阿里云百炼 API Key（必须）
export DASHSCOPE_API_KEY=sk-xxx
```

**可选配置（OpenAI）**：

```bash
# LLM 配置 - OpenAI
REME_LLM_BACKEND=openai
REME_LLM_MODEL_NAME=gpt-4o

# Embedding 配置 - OpenAI
REME_EMBEDDING_BACKEND=openai
REME_EMBEDDING_MODEL_NAME=text-embedding-3-large

# OpenAI API Key（必须）
export OPENAI_API_KEY=sk-xxx
```

### 3. 启动服务

```bash
# 方式 1：使用启动脚本
python start.py

# 方式 2：直接使用 uvicorn
cd src
uvicorn app.main:app --host 0.0.0.0 --port 8085
```

### 4. 验证服务

```bash
# 检查健康状态
curl http://localhost:8085/healthz

# 如果使用 API Key 认证
curl -H "Authorization: Bearer your-secret-key" http://localhost:8085/healthz
```

## 与 Java Agent 集成

### 1. 配置 Java Agent

在 `application-agentic.yaml` 中添加：

```yaml
tdagent:
  reme:
    enabled: true
    base-url: http://localhost:8085
    api-key: your-secret-key  # 如果启用了认证
    timeout-seconds: 60
    top-k: 5
```

### 2. 环境变量方式

```bash
export TDAGENT_REME_BASE_URL=http://localhost:8085
export TDAGENT_REME_API_KEY=your-secret-key
```

## 认证机制

### 启用认证

设置 `REME_API_KEY` 环境变量：

```bash
export REME_API_KEY=your-secret-key
```

### 客户端认证

客户端需要在请求头中提供：

```
Authorization: Bearer your-secret-key
```

或

```
Authorization: your-secret-key
```

### 禁用认证

不设置 `REME_API_KEY` 环境变量即可禁用认证。

## 故障排除

### 404 Not Found

**问题**: 请求 `/summary_personal_memory` 或 `/retrieve_personal_memory` 返回 404

**原因**: 服务端未实现官方 API 端点

**解决**: 确保使用本修复版本的 `router.py`

### 401/403 Authorization Error

**问题**: 返回 401 或 403 错误

**原因**:

- 401: 缺少 Authorization 头
- 403: API Key 不正确

**解决**:

1. 检查是否设置了 `REME_API_KEY`
2. 客户端请求头中添加正确的 API Key
3. 或者移除 `REME_API_KEY` 禁用认证

### Chroma 连接失败

**问题**: Chroma 向量数据库连接失败

**原因**: Chroma 服务未启动或配置错误

**解决**:

1. 启动 Chroma: `chroma run --path .chroma --port 8000`
2. 或者使用本地向量后端: `REME_VECTOR_BACKEND=local`

### LLM/Embedding API Key 未配置

**问题**: LLM 或 Embedding 调用失败

**原因**: 未配置相应的 API Key

**解决**:

```bash
# OpenAI
export OPENAI_API_KEY=sk-xxx

# DashScope (阿里云)
export DASHSCOPE_API_KEY=sk-xxx
```

## 技术栈

- **框架**: FastAPI
- **向量数据库**: Chroma (可选)
- **LLM 后端**: OpenAI API 兼容 (DashScope, OpenAI, 等)
- **Embedding**: OpenAI API 兼容
- **认证**: API Key (Bearer Token)

## 许可证

Apache 2.0
