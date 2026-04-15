# ReMe Server 阿里云百炼配置指南

## 📚 目录

- [阿里云百炼简介](#阿里云百炼简介)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [模型选择](#模型选择)
- [常见问题](#常见问题)

---

## 🤖 阿里云百炼简介

**阿里云百炼**（DashScope）是阿里云提供的大模型服务平台，提供多种通义千问（Qwen）模型及其他 AI 模型。

### 优势

- ✅ **国内访问速度快** - 服务器位于中国大陆
- ✅ **价格优惠** - 相比 OpenAI 更便宜
- ✅ **中文支持好** - 对中文理解和生成更优
- ✅ **合规性** - 符合中国数据合规要求

---

## 🚀 快速开始

### 1. 获取 API Key

1. 访问 [阿里云百炼控制台](https://dashscope.console.aliyun.com/)
2. 登录阿里云账号
3. 进入 **API Key 管理** 页面
4. 创建新的 API Key
5. 复制并保存 API Key（只显示一次）

### 2. 配置 ReMe Server

在 `reme-server/.env` 文件中：

```bash
# LLM 配置 - 阿里云百炼
REME_LLM_BACKEND=dashscope
REME_LLM_MODEL_NAME=qwen-plus

# Embedding 配置 - 阿里云百炼
REME_EMBEDDING_BACKEND=dashscope
REME_EMBEDDING_MODEL_NAME=text-embedding-v3
REME_EMBEDDING_DIMENSIONS=1536

# 向量后端（推荐使用 local，无需 Chroma）
REME_VECTOR_BACKEND=local

# 阿里云百炼 API Key（必须）
export DASHSCOPE_API_KEY=sk-xxxxxxxxxxxxxxxx
```

### 3. 启动服务

```bash
cd reme-server
python start.py
```

### 4. 验证配置

```bash
# 检查健康状态
curl http://localhost:8085/healthz

# 预期输出应包含：
# {
#   "ok": true,
#   "reme": {
#     "ready": true,
#     "llm": {
#       "backend": "dashscope",
#       "model_name": "qwen-plus"
#     },
#     "embedding": {
#       "backend": "dashscope",
#       "model_name": "text-embedding-v3"
#     }
#   }
# }
```

---

## ⚙️ 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 | 必填 |
|-------|------|--------|------|
| `DASHSCOPE_API_KEY` | 阿里云百炼 API Key | - | ✅ 是 |
| `REME_LLM_BACKEND` | LLM 后端类型 | `dashscope` | ❌ 否 |
| `REME_LLM_MODEL_NAME` | LLM 模型名称 | `qwen-plus` | ❌ 否 |
| `REME_EMBEDDING_BACKEND` | Embedding 后端类型 | `dashscope` | ❌ 否 |
| `REME_EMBEDDING_MODEL_NAME` | Embedding 模型名称 | `text-embedding-v3` | ❌ 否 |
| `REME_EMBEDDING_DIMENSIONS` | Embedding 维度 | `1536` | ❌ 否 |
| `REME_VECTOR_BACKEND` | 向量后端类型 | `local` | ❌ 否 |

### 配置示例

#### 基础配置（仅 LLM）

```bash
export DASHSCOPE_API_KEY=sk-xxx
REME_LLM_BACKEND=dashscope
REME_LLM_MODEL_NAME=qwen-plus
```

#### 完整配置（LLM + Embedding）

```bash
export DASHSCOPE_API_KEY=sk-xxx
REME_LLM_BACKEND=dashscope
REME_LLM_MODEL_NAME=qwen-plus
REME_EMBEDDING_BACKEND=dashscope
REME_EMBEDDING_MODEL_NAME=text-embedding-v3
REME_EMBEDDING_DIMENSIONS=1536
REME_VECTOR_BACKEND=local
```

#### 生产环境配置（带认证）

```bash
export DASHSCOPE_API_KEY=sk-xxx
REME_API_KEY=your-secret-key
REME_LLM_BACKEND=dashscope
REME_LLM_MODEL_NAME=qwen-max
REME_EMBEDDING_BACKEND=dashscope
REME_EMBEDDING_MODEL_NAME=text-embedding-v3
REME_VECTOR_BACKEND=chroma
REME_CHROMA_HOST=127.0.0.1
REME_CHROMA_PORT=8000
```

---

## 🎯 模型选择

### LLM 模型

| 模型名称 | 说明 | 适用场景 | 价格（约） |
|---------|------|---------|-----------|
| `qwen-turbo` | 速度快，成本低 | 简单任务、快速响应 | ¥0.002/1K tokens |
| `qwen-plus` | 性能均衡（推荐） | 大多数场景 | ¥0.004/1K tokens |
| `qwen-max` | 性能最强 | 复杂推理、代码生成 | ¥0.04/1K tokens |
| `qwen-long` | 长文本支持 | 长文档处理 | ¥0.005/1K tokens |

**推荐**：使用 `qwen-plus`，性能和成本的平衡最佳。

### Embedding 模型

| 模型名称 | 维度 | 说明 | 适用场景 |
|---------|------|------|---------|
| `text-embedding-v3` | 1536 | 最新版本（推荐） | 通用场景 |
| `text-embedding-v2` | 1536 | 上一版本 | 兼容性需求 |
| `text-embedding-v1` | 1536 | 早期版本 | 不推荐 |

**推荐**：使用 `text-embedding-v3`，效果最好。

---

## ❓ 常见问题

### 1. API Key 无效

**错误信息**：
```
dashscope.APIError: Invalid API key
```

**解决方案**：
1. 检查 `DASHSCOPE_API_KEY` 是否正确设置
2. 确认 API Key 未过期
3. 确认账号有足够的余额

### 2. 模型不存在

**错误信息**：
```
dashscope.ModelNotFound: Model not found
```

**解决方案**：
1. 检查模型名称是否正确
2. 确认该模型在你的账号中可用
3. 查看 [百炼控制台](https://dashscope.console.aliyun.com/) 的模型列表

### 3. 余额不足

**错误信息**：
```
dashscope.BillingError: Insufficient balance
```

**解决方案**：
1. 登录阿里云控制台
2. 充值账号余额
3. 检查是否开通了百炼服务

### 4. 访问速度慢

**解决方案**：
1. 使用国内节点（阿里云百炼已在国内）
2. 检查网络连接
3. 考虑使用本地向量后端（`REME_VECTOR_BACKEND=local`）

### 5. Embedding 维度不匹配

**错误信息**：
```
ValueError: Dimension mismatch
```

**解决方案**：
```bash
# text-embedding-v3 使用 1536 维度
REME_EMBEDDING_DIMENSIONS=1536

# text-embedding-v2 使用 1536 维度
REME_EMBEDDING_DIMENSIONS=1536
```

---

## 💰 价格参考

### LLM 模型价格（2024 年）

| 模型 | 输入价格 | 输出价格 |
|------|---------|---------|
| qwen-turbo | ¥0.002/1K tokens | ¥0.006/1K tokens |
| qwen-plus | ¥0.004/1K tokens | ¥0.012/1K tokens |
| qwen-max | ¥0.04/1K tokens | ¥0.12/1K tokens |

### Embedding 模型价格

| 模型 | 价格 |
|------|------|
| text-embedding-v3 | ¥0.0005/1K tokens |
| text-embedding-v2 | ¥0.0007/1K tokens |

**注意**：价格可能变动，请以 [阿里云百炼官网](https://help.aliyun.com/zh/dashscope/pricing) 为准。

---

## 🔗 相关资源

- [阿里云百炼官网](https://www.aliyun.com/product/bailian)
- [百炼控制台](https://dashscope.console.aliyun.com/)
- [API 文档](https://help.aliyun.com/zh/dashscope/)
- [价格详情](https://help.aliyun.com/zh/dashscope/pricing)
- [模型列表](https://help.aliyun.com/zh/dashscope/model-list)

---

## 📞 技术支持

如有问题，请查阅：

1. 阿里云百炼官方文档
2. ReMe Server 文档
3. 项目 Issue 列表
