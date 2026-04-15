# ReMe Server API Key 认证配置指南

## 📚 目录

- [REME_API_KEY 是什么？](#reme_api_key-是什么)
- [为什么需要配置？](#为什么需要配置)
- [配置方法](#配置方法)
- [认证流程](#认证流程)
- [常见错误及解决方案](#常见错误及解决方案)
- [安全建议](#安全建议)

---

## 🔐 REME_API_KEY 是什么？

`REME_API_KEY` 是 **ReMe Server 的服务访问密钥**，用于保护 ReMe 服务不被未授权访问。

### 作用

- ✅ **身份验证**: 确保只有授权的客户端才能访问 ReMe 服务
- ✅ **防止未授权访问**: 阻止恶意用户或程序调用你的记忆服务
- ✅ **访问控制**: 可以针对不同客户端配置不同的 API Key

### 工作原理

```
┌─────────────────┐                      ┌─────────────────┐
│  agent-engine   │                      │   reme-server   │
│  (Java 客户端)   │                      │  (Python 服务)  │
└────────┬────────┘                      └────────┬────────┘
         │                                        │
         │  POST /summary_personal_memory         │
         │  Header: Authorization: Bearer xxx     │
         │ ─────────────────────────────────────> │
         │                                        │
         │                                        │ 1. 检查是否设置 REME_API_KEY
         │                                        │ 2. 验证 Authorization 头
         │                                        │ 3. 比对 API Key 是否匹配
         │                                        │
         │  200 OK (认证成功)                     │
         │  或 401/403 (认证失败)                 │
         │ <───────────────────────────────────── │
         │                                        │
└─────────┴────────┘                      └────────┴────────┘
```

---

## ❓ 为什么需要配置？

### 两个关键配置

| 配置位置 | 配置项 | 作用 | 是否必须 |
|---------|-------|------|---------|
| **reme-server** | `REME_API_KEY` | 服务端验证密钥（防守方） | ❌ 可选（不设置则禁用认证） |
| **agent-engine** | `tdagent.reme.api-key` | 客户端发送密钥（进攻方） | ❌ 可选（仅当服务端启用认证时需要） |

### ⚠️ 重要：两个配置必须一致

```bash
# reme-server 的 .env 文件
REME_API_KEY=my-secret-key-123
```

```yaml
# agent-engine 的 application-agentic.yaml
tdagent:
  reme:
    base-url: http://localhost:8085
    api-key: my-secret-key-123  # 必须与 REME_API_KEY 相同！
```

---

## 🛠️ 配置方法

### 场景 1：启用认证（推荐生产环境）

#### 1. 配置 ReMe Server

在 `reme-server/.env` 文件中：

```bash
# 设置 API Key（建议使用强随机字符串，至少 16 个字符）
REME_API_KEY=super-secret-key-123

# LLM 配置 - 阿里云百炼
REME_LLM_BACKEND=dashscope
REME_LLM_MODEL_NAME=qwen-plus

# Embedding 配置 - 阿里云百炼
REME_EMBEDDING_BACKEND=dashscope
REME_EMBEDDING_MODEL_NAME=text-embedding-v3

# 阿里云百炼 API Key（必须）
export DASHSCOPE_API_KEY=sk-xxx

# 其他配置...
REME_WORKING_DIR=.reme
REME_VECTOR_BACKEND=local
```

#### 2. 配置 Java Agent Engine

在 `application-agentic.yaml` 或环境变量中：

```yaml
tdagent:
  reme:
    enabled: true
    base-url: http://localhost:8085
    api-key: super-secret-key-123  # 必须与 REME_API_KEY 一致！
    timeout-seconds: 60
    top-k: 5
```

或使用环境变量：

```bash
export TDAGENT_REME_API_KEY=super-secret-key-123
```

#### 3. 验证配置

```bash
# 测试认证访问
curl -X POST http://localhost:8085/summary_personal_memory \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer super-secret-key-123" \
  -d '{"workspace_id": "test", "trajectories": []}'
```

### 场景 2：禁用认证（本地开发）

#### 1. ReMe Server 不设置 API Key

```bash
# .env 文件中不设置 REME_API_KEY，或直接删除该行
# REME_API_KEY=xxx  <-- 注释掉或删除
```

#### 2. Java Agent Engine 无需配置

```yaml
tdagent:
  reme:
    enabled: true
    base-url: http://localhost:8085
    # api-key 不需要配置
```

---

## 🔄 认证流程

### 服务端认证逻辑（Python）

查看 `src/app/main.py` 的认证中间件：

```python
API_KEY = os.getenv("REME_API_KEY")  # 从环境变量读取


async def verify_auth(request: Request, call_next):
    # 1. 如果未配置 API Key，跳过认证
    if not API_KEY:
        return await call_next(request)

    # 2. 跳过健康检查和心跳检测
    if request.url.path in ["/healthz", "/heartbeat"]:
        return await call_next(request)

    # 3. 验证 Authorization 头
    auth_header = request.headers.get("Authorization")
    if not auth_header:
        return JSONResponse(status_code=401, detail="Missing Authorization header")

    # 4. 支持两种格式：Bearer <key> 或 <key>
    auth_value = auth_header
    if auth_header.startswith("Bearer "):
        auth_value = auth_header[7:]

    # 5. 比对 API Key
    if auth_value != API_KEY:
        return JSONResponse(status_code=403, detail="Invalid API key")

    return await call_next(request)
```

### 客户端发送认证（Java）

查看 `ReMeClient.java` 的认证头添加逻辑：

```java
.addInterceptor(chain -> {
    Request original = chain.request();
    Request.Builder requestBuilder = original.newBuilder()
            .header("Content-Type", "application/json; charset=utf-8");
    // 如果配置了 apiKey，添加 Authorization 头
    if (apiKey != null && !apiKey.isEmpty()) {
        requestBuilder.header("Authorization", "Bearer " + apiKey);
    }
    return chain.proceed(requestBuilder.build());
})
```

---

## ❌ 常见错误及解决方案

| 错误状态码 | 错误信息 | 原因 | 解决方案 |
|-----------|---------|------|---------|
| `401` | `Missing Authorization header` | 服务端启用了认证，但客户端没发送 API Key | 在 agent-engine 配置中添加 `api-key` |
| `403` | `Invalid API key` | 两个配置的 Key 不一致 | 确保 `REME_API_KEY` = `tdagent.reme.api-key` |
| `404` | `Not Found` | API 端点不匹配 | 使用修复后的 `router.py` |
| `Connection refused` | - | reme-server 未启动 | 启动 `python start.py` |

### 详细错误示例

#### 错误 1：401 Missing Authorization header

```bash
# 服务端配置
REME_API_KEY=my-secret-key

# 客户端请求（缺少 Authorization 头）
curl -X POST http://localhost:8085/summary_personal_memory \
  -H "Content-Type: application/json" \
  -d '{"workspace_id": "test"}'

# 返回：401 Unauthorized
{"detail": "Missing Authorization header"}
```

**解决方案**: 添加 Authorization 头

```bash
curl -X POST http://localhost:8085/summary_personal_memory \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer my-secret-key" \
  -d '{"workspace_id": "test"}'
```

#### 错误 2：403 Invalid API key

```bash
# 服务端配置
REME_API_KEY=correct-key-123

# 客户端请求（使用错误的 Key）
curl -X POST http://localhost:8085/summary_personal_memory \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer wrong-key-456" \
  -d '{"workspace_id": "test"}'

# 返回：403 Forbidden
{"detail": "Invalid API key"}
```

**解决方案**: 确保两个配置一致

```bash
# 服务端
REME_API_KEY=correct-key-123

# 客户端
api-key: correct-key-123
```

---

## 🔒 安全建议

### 不同环境的推荐配置

| 环境 | 推荐配置 |
|-----|---------|
| **本地开发** | ❌ 禁用认证（不设置 `REME_API_KEY`）<br>方便调试，无需频繁输入密钥 |
| **测试环境** | ✅ 启用认证，使用简单 Key<br>模拟生产环境行为 |
| **生产环境** | ✅ **必须启用认证**<br>✅ 使用强随机 Key（32+ 字符）<br>✅ 通过环境变量或密钥管理服务存储<br>✅ 定期轮换 Key |

### 生成强随机 API Key

```bash
# 方法 1：使用 OpenSSL
openssl rand -hex 16

# 方法 2：使用 /dev/urandom
cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1

# 方法 3：使用 Python
python -c "import secrets; print(secrets.token_hex(16))"

# 方法 4：使用 PowerShell (Windows)
-join ((65..90) + (97..122) + (48..57) | Get-Random -Count 32 | ForEach-Object {[char]$_})
```

### 密钥存储最佳实践

1. ✅ **使用环境变量**: 不要将密钥硬编码在代码中
2. ✅ **使用 .env 文件**: 将 `.env` 添加到 `.gitignore`
3. ✅ **使用密钥管理服务**: 如 AWS Secrets Manager、HashiCorp Vault
4. ✅ **定期轮换**: 定期更换 API Key，降低泄露风险

---

## 🧪 验证配置是否正确

### 测试脚本

```bash
#!/bin/bash

# 配置
API_KEY="your-api-key"
BASE_URL="http://localhost:8085"

echo "=== ReMe Server 认证测试 ==="

# 测试 1：健康检查（无需认证）
echo -e "\n1. 测试健康检查（无需认证）"
curl -s $BASE_URL/healthz | jq .

# 测试 2：无认证访问 API（应该返回 401/403）
echo -e "\n2. 测试无认证访问 API"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST $BASE_URL/summary_personal_memory \
  -H "Content-Type: application/json" \
  -d '{"workspace_id": "test"}'

# 测试 3：有认证访问 API（应该返回 200）
echo -e "\n3. 测试有认证访问 API"
curl -s -w "\nHTTP Status: %{http_code}\n" \
  -X POST $BASE_URL/summary_personal_memory \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $API_KEY" \
  -d '{"workspace_id": "test", "trajectories": []}'

echo -e "\n=== 测试完成 ==="
```

### 预期输出

```
=== ReMe Server 认证测试 ===

1. 测试健康检查（无需认证）
{"ok":true,"reme":{"ready":true,...}}

2. 测试无认证访问 API
{"detail":"Missing Authorization header"}
HTTP Status: 401

3. 测试有认证访问 API
{"answer":null,"success":true,"metadata":{"workspace_id":"test"}}
HTTP Status: 200

=== 测试完成 ===
```

---

## 📖 相关文档

- [README.md](README.md) - ReMe Server 主文档
- [.env.example](.env.example) - 配置模板
- [src/app/main.py](src/app/main.py) - 认证中间件实现
- [src/app/router.py](src/app/router.py) - API 端点实现

---

## 📞 技术支持

如有问题，请查阅：

1. ReMe 官方文档：https://gitee.com/chenfei6095/ReMe
2. 项目 Issue 列表
3. 开发团队联系方式
