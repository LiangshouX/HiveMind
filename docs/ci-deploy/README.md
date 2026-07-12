# HiveMind CI/CD 与部署指南

> 使用 Docker 和 GitHub Actions 构建、测试和部署 HiveMind 的完整指南。

[English Version](./README_EN.md)

---

## 目录

1. [系统架构](#1-系统架构)
2. [环境要求](#2-环境要求)
3. [快速启动（Docker Compose）](#3-快速启动docker-compose)
4. [配置参考](#4-配置参考)
5. [GitHub Actions CI/CD](#5-github-actions-cicd)
6. [手动构建 Docker 镜像](#6-手动构建-docker-镜像)
7. [生产环境部署](#7-生产环境部署)
8. [常见问题排查](#8-常见问题排查)

---

## 1. 系统架构

HiveMind 由 5 个服务组成：

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Nginx     │────▶│  Java App   │────▶│   MySQL     │
│  (前端)     │     │  (Spring    │     │   8.0       │
│   :80       │     │   Boot)     │     │   :3306     │
└─────────────┘     │   :8080     │     └─────────────┘
                    │             │
                    │             │────▶┌─────────────┐
                    │             │     │  MongoDB    │
                    │             │     │   7.0       │
                    │             │     │   :27017    │
                    │             │     └─────────────┘
                    │             │
                    │             │────▶┌─────────────┐
                    └─────────────┘     │  ReMe       │
                                        │  Server     │
                                        │  (Python)   │
                                        │   :8002     │
                                        └─────────────┘
```

| 服务 | 技术栈 | 端口 | Docker 镜像 |
|------|--------|------|-------------|
| **web** | Nginx + React | 80 | `hivemind-web` |
| **hivemind** | Java 17 + Spring Boot 4.0.3 | 8080 | `hivemind-app` |
| **mysql** | MySQL 8.0 | 3306 | `mysql:8.0` |
| **mongodb** | MongoDB 7.0 | 27017 | `mongo:7.0` |
| **reme-server** | Python 3.11 + FastAPI | 8002 | `hivemind-reme` |

---

## 2. 环境要求

### Docker 部署（推荐）

- **Docker** 20.10+（需支持 Docker Compose V2）
- **Git** 用于克隆仓库
- 一个 **LLM API Key**（DashScope / DeepSeek / OpenAI 兼容接口）

### 本地开发

- **JDK 17+**（推荐 Eclipse Temurin）
- **Maven** 3.9+
- **Node.js** 20+（含 npm）
- **Python** 3.11+（用于 ReMe 服务）
- **MySQL** 8.0+
- **MongoDB** 6.0+

---

## 3. 快速启动（Docker Compose）

### 3.1 克隆仓库

```bash
git clone https://github.com/LiangshouX/HiveMind.git
cd HiveMind
```

### 3.2 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env` 文件，填入你的 API Key：

```bash
# 必填：LLM API Key
AI_DASHSCOPE_API_KEY=sk-你的API密钥

# ReMe 记忆服务所需的 LLM 配置
FLOW_LLM_API_KEY=sk-你的API密钥
FLOW_LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
```

### 3.3 启动所有服务

```bash
# 构建并启动所有服务
docker compose up -d

# 查看所有服务日志
docker compose logs -f

# 查看某个服务的日志
docker compose logs -f hivemind
```

### 3.4 访问应用

| 服务 | 地址 |
|------|------|
| **前端控制台** | http://localhost |
| **后端 API** | http://localhost:8080 |
| **Swagger 文档** | http://localhost:8080/swagger-ui.html |
| **ReMe 服务** | http://localhost:8002 |

### 3.5 首次使用

1. 在浏览器打开 http://localhost
2. 注册一个新账号
3. 在管理面板配置 LLM 模型（或使用默认配置）
4. 开始与 AI 助理对话！

### 3.6 停止服务

```bash
# 停止所有服务
docker compose down

# 停止并删除数据卷（⚠️ 会删除所有数据）
docker compose down -v
```

---

## 4. 配置参考

### 4.1 环境变量

所有可配置的环境变量定义在 `.env.example` 中。以下是完整参考：

#### 数据库

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `MYSQL_ROOT_PASSWORD` | `hivemind123` | MySQL root 密码 |
| `SPRING_DATASOURCE_URL` | （compose 中自动设置） | MySQL JDBC 连接 URL |
| `MONGODB_URI` | （compose 中自动设置） | MongoDB 连接 URI |

#### 安全

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `JWT_SECRET` | `HiveMindJwtSecretKey2026...` | JWT 签名密钥（生产环境务必修改！） |

#### LLM 配置

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `AI_DASHSCOPE_API_KEY` | （空） | LLM API 密钥（必填） |
| `TDAGENT_MODEL_PROVIDER` | `dashscope` | 供应商：`dashscope` / `deepseek` / `openai` |
| `TDAGENT_MODEL_NAME` | `qwen-max` | 模型显示名称 |
| `TDAGENT_MODEL_ID` | `qwen3-max` | API 调用的模型 ID |
| `TDAGENT_MODEL_BASE_URL` | `https://dashscope.aliyuncs.com` | API 基础 URL |

#### ReMe 记忆服务

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `FLOW_LLM_API_KEY` | （必填） | ReMe LLM 操作的 API 密钥 |
| `FLOW_LLM_BASE_URL` | `https://dashscope.aliyuncs.com/compatible-mode/v1` | LLM API 地址 |
| `FLOW_EMBEDDING_API_KEY` | （同 LLM） | Embedding 模型 API 密钥 |
| `FLOW_EMBEDDING_BASE_URL` | （同 LLM） | Embedding 模型地址 |

### 4.2 配置文件

| 文件 | 用途 |
|------|------|
| `application.yaml` | Spring Boot 主配置（端口、Profile） |
| `application-backend.yaml` | MySQL、MyBatis-Plus、JWT、Swagger 配置 |
| `application-agentic.yaml` | Agent 引擎：模型、沙箱、Tool Guard、ReMe、记忆压缩 |
| `reme-server/.env` | ReMe 服务环境变量 |
| `provider/builtin_provider.json` | LLM 供应商定义 |

### 4.3 支持的 LLM 供应商

| 供应商 | Provider ID | 示例模型 | Base URL |
|--------|------------|----------|----------|
| 阿里云百炼 | `dashscope` | qwen-max, qwen-plus, qwen-turbo | `https://dashscope.aliyuncs.com` |
| DeepSeek | `deepseek` | deepseek-chat, deepseek-reasoner | `https://api.deepseek.com` |
| OpenAI 兼容 | `openai` | gpt-4o, gpt-4o-mini | `https://api.openai.com` |

---

## 5. GitHub Actions CI/CD

### 5.1 CI 流水线（`.github/workflows/ci.yml`）

每次推送到 `main`/`develop` 分支或向 `main` 发起 Pull Request 时触发。

**包含的 Job：**

1. **Java Build & Test** — 构建 Maven 项目，使用 MySQL + MongoDB Service 运行全部测试
2. **Frontend Build & Lint** — 安装 npm 依赖，运行 ESLint，构建 TypeScript + Vite
3. **Docker Build Verify** — 构建全部 3 个 Docker 镜像，验证 Dockerfile 可用

### 5.2 发布流水线（`.github/workflows/release.yml`）

推送版本标签时触发：

```bash
# 创建并推送发布标签
git tag v1.0.0
git push origin v1.0.0
```

自动执行：
1. 构建 3 个 Docker 镜像
2. 推送到 GitHub Container Registry（GHCR），同时标记版本号和 `latest`

**发布的镜像：**
- `ghcr.io/liangshoux/hivemind-app:latest`
- `ghcr.io/liangshoux/hivemind-web:latest`
- `ghcr.io/liangshoux/hivemind-reme:latest`

### 5.3 使用预构建镜像

发布后，其他用户无需本地构建即可运行：

```bash
# 下载生产环境 compose 文件
curl -O https://raw.githubusercontent.com/LiangshouX/HiveMind/main/docker-compose.prod.yml
curl -O https://raw.githubusercontent.com/LiangshouX/HiveMind/main/.env.example

# 配置
cp .env.example .env
# 编辑 .env 填入 API Key

# 使用预构建镜像运行
docker compose -f docker-compose.prod.yml up -d
```

---

## 6. 手动构建 Docker 镜像

### 6.1 Java 后端

```bash
# 在项目根目录执行
docker build -t hivemind-app:latest .
```

Dockerfile 使用多阶段构建：
- **第一阶段**：Maven 构建 fat JAR
- **第二阶段**：JRE Alpine 运行应用

### 6.2 前端

```bash
# 在 website/ 目录执行
docker build -t hivemind-web:latest ./website
```

Dockerfile 使用多阶段构建：
- **第一阶段**：Node.js 构建 React 应用
- **第二阶段**：Nginx 提供静态文件服务 + 反向代理

### 6.3 ReMe 服务

```bash
# 在 reme-server/ 目录执行
docker build -t hivemind-reme:latest -f docker/Dockerfile ./reme-server
```

---

## 7. 生产环境部署

### 7.1 安全检查清单

- [ ] 修改 `JWT_SECRET` 为强随机字符串
- [ ] 修改 `MYSQL_ROOT_PASSWORD` 为强密码
- [ ] 启用 HTTPS（通过 Nginx 或反向代理添加 SSL 终结）
- [ ] 限制数据库端口（不要公开暴露 3306/27017）
- [ ] 使用 Docker Secrets 或密钥管理服务存储 API Key
- [ ] 设置 `TDAGENT_SANDBOX_ENABLED=false`（除非已配置 Docker-in-Docker）

### 7.2 资源需求

| 服务 | 最低内存 | 推荐内存 | CPU |
|------|---------|---------|-----|
| Java 应用 | 512MB | 1-2GB | 2 核 |
| MySQL | 256MB | 512MB | 1 核 |
| MongoDB | 256MB | 512MB | 1 核 |
| ReMe 服务 | 256MB | 512MB | 1 核 |
| Nginx | 64MB | 128MB | 1 核 |
| **合计** | **~1.3GB** | **~2.7GB** | **6 核** |

### 7.3 数据持久化

Docker Compose 配置使用命名卷：

| 卷名 | 用途 |
|------|------|
| `mysql-data` | MySQL 数据库文件 |
| `mongo-data` | MongoDB 数据库文件 |
| `reme-workspace` | ReMe 记忆数据 |

生产环境建议使用宿主机目录挂载：

```yaml
volumes:
  - /data/hivemind/mysql:/var/lib/mysql
  - /data/hivemind/mongodb:/data/db
  - /data/hivemind/reme:/workspace
```

### 7.4 反向代理（HTTPS）

生产环境建议将 HiveMind 部署在带 SSL 的反向代理后面：

```nginx
# /etc/nginx/sites-available/hivemind
server {
    listen 443 ssl;
    server_name hivemind.example.com;

    ssl_certificate /etc/letsencrypt/live/hivemind.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/hivemind.example.com/privkey.pem;

    location / {
        proxy_pass http://localhost:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # SSE 流式支持
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_buffering off;
        proxy_read_timeout 300s;
    }
}
```

---

## 8. 常见问题排查

### 8.1 常见问题

#### MySQL 启动失败

```bash
# 查看日志
docker compose logs mysql

# 常见解决方法：删除旧数据卷
docker compose down -v
docker compose up -d
```

#### Java 应用无法连接 MySQL

确保 MySQL 健康检查通过后再启动应用：
```bash
docker compose ps          # 查看 mysql 健康状态
docker compose logs hivemind  # 查看应用日志
```

#### ReMe 服务启动崩溃

```bash
# 检查 API Key 是否正确配置
docker compose logs reme-server

# 常见原因：FLOW_LLM_API_KEY 未设置或无效
```

#### 前端显示 "Network Error"

Nginx 容器代理到 `hivemind:8080`。请确保：
1. `hivemind` 服务正在运行且健康
2. API 请求路径以 `/api/` 开头

### 8.2 常用命令

```bash
# 查看所有服务状态
docker compose ps

# 重启某个服务
docker compose restart hivemind

# 进入容器调试
docker compose exec hivemind sh
docker compose exec mysql mysql -u root -p

# 查看资源使用情况
docker stats

# 清理所有资源
docker compose down -v --rmi all
```

### 8.3 查看日志

```bash
# 所有服务
docker compose logs -f

# 指定服务
docker compose logs -f hivemind

# 最近 100 行
docker compose logs --tail=100 hivemind
```

---

## 附录：文件结构

```
HiveMind/
├── .github/
│   └── workflows/
│       ├── ci.yml              # CI 流水线（测试 + 构建）
│       └── release.yml         # 发布流水线（Docker 推送）
├── .env.example                # 环境变量模板
├── .dockerignore               # Docker 构建上下文排除规则
├── Dockerfile                  # Java 后端多阶段构建
├── docker-compose.yml          # 全栈编排（从源码构建）
├── docker-compose.prod.yml     # 全栈编排（使用预构建镜像）
├── docs/
│   └── ci-deploy/
│       ├── README.md           # 本文档（中文）
│       └── README_EN.md        # English version
├── hivemind-agent-engine/      # Agent 引擎模块
├── hivemind-backend/           # 业务后端模块
├── hivemind-launcher/          # Spring Boot 启动入口
├── reme-server/
│   ├── docker/
│   │   ├── Dockerfile          # ReMe 服务构建
│   │   └── docker-compose.yml  # ReMe 独立编排
│   └── .dockerignore           # ReMe 构建排除规则
├── website/
│   ├── Dockerfile              # 前端多阶段构建
│   └── nginx.conf              # Nginx 配置
└── pom.xml                     Maven 父工程
```
