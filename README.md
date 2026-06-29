# TangDynasty

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![AgentScope](https://img.shields.io/badge/AgentScope-1.0.12-blue.svg)](https://agentscope.io/)

**TangDynasty** 是一个基于多 Agent 协作架构的云端 AI 助理平台，通过制度化、流程化的任务流转机制，实现复杂的多 Agent 协作工作流。

---

## 📖 简介

TangDynasty 构建了一个层次分明、职责清晰的多 Agent 协作系统。系统能够自动将复杂任务拆解为子任务，分配给不同的 Agent 执行，并通过审查机制确保执行质量。

### 核心理念

- **制度化协作**：建立规范的任务流转机制
- **职责分离**：不同 Agent 承担不同角色，各司其职
- **安全可控**：Tool Guard 机制确保工具调用的安全性
- **长期记忆**：ReMe 支持跨会话的记忆检索与复用

---

## ✨ 核心特性

| 特性 | 描述 |
|------|------|
| 🤖 **多Agent协作** | 基于角色分工的任务编排系统 |
| 🧠 **ReAct Agent** | 推理 + 行动的 Agent 执行模式 |
| 🛡️ **Tool Guard** | 工具调用安全防护和审批机制 |
| 💾 **长期记忆** | ReMe 跨会话记忆检索 |
| 🔌 **Skill 系统** | 用户自定义技能扩展 |
| 🔗 **MCP 集成** | Model Context Protocol 支持 |
| 📱 **多渠道接入** | 钉钉、飞书、Discord 等 |
| ⏰ **定时任务** | Cron 计划任务 |
| 📊 **Token 统计** | 模型调用费用统计 |

---

## 🏗️ 多Agent协作流程

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户                                      │
│                    发送任务 / 闲聊消息                            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Triage Agent (AGENT_TRIAGE)                    │
│              消息分拣：识别任务 or 闲聊                           │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              │ 任务                          │ 闲聊
              ▼                               ▼
┌─────────────────────────┐         ┌─────────────────┐
│  Planner Agent           │         │   直接回复       │
│  (AGENT_PLANNER)         │         │                 │
│  任务规划：拆解子任务     │         │                 │
└─────────────────────────┘         └─────────────────┘
              │
              ▼
┌─────────────────────────┐
│  Reviewer Agent          │
│  (AGENT_REVIEWER)        │
│  审查：审查方案           │
│  通过→执行层             │
│  驳回→规划层 (最多 3 轮)  │
└─────────────────────────┘
              │
              ▼
┌─────────────────────────┐
│  Executor Agent          │
│  (AGENT_EXECUTOR)        │
│  执行：派发执行           │
│  汇总结果→规划层         │
└─────────────────────────┘
              │
              ▼
┌─────────────────────────┐
│   报告呈报              │
│   用户审阅              │
└─────────────────────────┘
```

---

## 🚀 快速开始

### 环境要求

| 组件 | 最低版本 | 推荐版本 |
|------|---------|---------|
| **JDK** | 17 | 17+ |
| **Python** | 3.10 | 3.10-3.13 |
| **Node.js** | 18 | 20+ |
| **MySQL** | 8.0 | 8.0.36+ |
| **MongoDB** | 6.0 | 7.0+ |
| **Docker** | 20.10 | 24.0+ |

### 安装步骤

#### 1. 克隆项目

```bash
git clone https://github.com/your-org/tang-dynasty.git
cd tang-dynasty
```

#### 2. 启动基础设施

```bash
# 启动 MySQL
docker run -d --name mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=tang_dynasty \
  -p 3306:3306 \
  mysql:8.0

# 启动 MongoDB
docker run -d --name mongodb \
  -p 27017:27017 \
  mongo:7.0
```

#### 3. 配置后端

```bash
# 复制配置文件
cp tang-dynasty-backend/src/main/resources/application-backend.yaml.example \
   tang-dynasty-backend/src/main/resources/application-backend.yaml

# 编辑配置文件，设置数据库连接和 API Key
```

#### 4. 启动 ReMe Server（可选，使用长期记忆时需要）

```bash
cd reme-server

# 配置环境变量
cp .env.example .env
# 编辑 .env 设置 DASHSCOPE_API_KEY 等

# 启动服务
python start.py
# 或使用 Docker
docker-compose up -d
```

#### 5. 构建并启动后端

```bash
# 根目录执行
mvn clean install

# 启动应用
mvn spring-boot:run -pl tang-dynasty-launcher
```

#### 6. 启动前端控制台

```bash
cd website
npm install
npm run dev
```

访问 `http://localhost:5173` 即可使用控制台。

---

## 📁 项目结构

```
tang-dynasty/
├── tang-dynasty-launcher/        # 🚀 启动模块（主入口）
├── tang-dynasty-agent-engine/    # 🤖 Agent 引擎核心
├── tang-dynasty-backend/         # 🏢 业务后端服务
├── website/                      # 💻 前端控制台
├── reme-server/                  # 🧠 Python 长期记忆服务
├── example/                      # 📝 示例代码
├── docs/                         # 📚 项目文档
└── pom.xml                       # Maven 父工程
```

### 模块说明

| 模块 | 职责 | 技术栈 |
|------|------|--------|
| **tang-dynasty-launcher** | 应用启动入口，聚合所有子模块 | Spring Boot |
| **tang-dynasty-agent-engine** | AI Agent 核心运行时，提供对话、工具调用、记忆管理等能力 | AgentScope + MongoDB |
| **tang-dynasty-backend** | 用户管理、任务管理、配置管理等业务服务 | Spring Boot + MyBatis Plus |
| **website** | Web 用户界面，提供与 AI 助理交互的所有功能 | React + TypeScript + Ant Design |
| **reme-server** | 提供与 AgentScope-ReMe 兼容的长期记忆检索服务 | FastAPI + ReMe |

---

## 📖 使用文档

### 基本用法

#### 1. 登录系统

访问 `http://localhost:5173`，使用默认账号登录（首次启动需注册）。

#### 2. 创建任务

在控制台输入你的需求，系统会自动识别任务类型并分配给相应的 Agent：

```
请帮我分析最近的项目代码，找出可以优化的地方
```

#### 3. 查看任务进度

在任务列表中可以查看任务的实时状态和流转记录。

#### 4. 配置模型

在设置页面配置 LLM 供应商和 API Key：

- 阿里云百炼（DashScope）
- OpenAI
- 其他兼容 OpenAI API 的供应商

### 高级用法

#### Tool Guard 配置

在 `application-agentic.yaml` 中配置工具防护规则：

```yaml
tdagent:
  tool-guard:
    enabled: true
    strict-mode: true
    guarded-tools:
      - name: "execute_shell"
        reason: "需要用户确认"
    denied-tools:
      - name: "delete_system_files"
        reason: "高危操作，禁止执行"
```

#### 自定义 Skill

在 `tang-dynasty-agent-engine/src/main/resources/skills/` 目录下创建自定义技能：

```json
{
  "name": "code_review",
  "description": "代码审查技能",
  "prompt": "你是一名资深代码审查专家...",
  "tools": ["read_file", "search_code"]
}
```

#### MCP 集成

配置 Model Context Protocol 以扩展 Agent 能力：

```yaml
tdagent:
  mcp:
    enabled: true
    clients:
      - name: "filesystem"
        type: "stdio"
        command: "mcp-server-filesystem"
        args: ["/home/user"]
```

---

## 🛡️ Tool Guard 安全体系

TangDynasty 提供三层工具调用防护机制：

```
┌─────────────────────────────────────────────────────────────┐
│                      Tool Guard 决策流程                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  用户请求 → Agent 推理 → 工具调用                            │
│                          │                                  │
│                          ▼                                  │
│              ┌───────────────────────┐                      │
│              │  检查 denied_tools    │                      │
│              │  (无条件禁止)          │                      │
│              └───────────┬───────────┘                      │
│                          │ 命中                             │
│                          ▼                                  │
│                    直接阻断 ❌                               │
│                                                             │
│              ┌───────────────────────┐                      │
│              │  检查 guarded_tools   │                      │
│              │  (需要审批)            │                      │
│              └───────────┬───────────┘                      │
│                          │ 命中规则                         │
│                          ▼                                  │
│              写入 pending_approval → 返回"需要批准"           │
│                          │                                  │
│                          ▼                                  │
│              用户输入 /approve                              │
│                          │                                  │
│                          ▼                                  │
│              验证参数一致性 → 消费审批 → 允许执行 ✅          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🧪 测试

```bash
# 运行单元测试
mvn test

# 运行集成测试（需要 Docker）
mvn verify

# 前端测试
cd website
npm test
```

---

## 📄 配置说明

### 核心配置文件

| 文件 | 路径 | 描述 |
|------|------|------|
| `application-agentic.yaml` | `tang-dynasty-agent-engine/src/main/resources/` | Agent 引擎配置 |
| `application-backend.yaml` | `tang-dynasty-backend/src/main/resources/` | 后端业务配置 |
| `.env` | `reme-server/` | ReMe Server 环境变量 |

### Agent 引擎配置示例

```yaml
tdagent:
  model:
    provider-id: dashscope
    model-name: qwen-max
    max-iters: 200
  sandbox:
    enabled: true
    browser-enabled: true
  tool-guard:
    enabled: true
    strict-mode: true
  reme:
    enabled: true
    base-url: http://localhost:8085
    top-k: 5
  compaction:
    trigger-message-count: 20
    keep-recent-messages: 8
```

---

## 📚 文档资源

| 文档 | 描述 |
|------|------|
| [架构设计](docs/ARCHITECTURE.md) | Agent 引擎架构文档 |
| [请求链路](docs/copaw/请求链路.md) | 请求处理流程 |
| [Sandbox 指南](docs/agentscope/SandBox.md) | 沙箱镜像配置 |
| [ReMe 认证](reme-server/AUTH.md) | API Key 认证配置 |
| [ReMe 说明](reme-server/README.md) | ReMe Server 使用文档 |

---

## 🔧 常见问题

### Q: 启动时提示 MongoDB 连接失败

确保 MongoDB 已启动且连接字符串正确：
```bash
docker ps | grep mongodb
# 检查 application-backend.yaml 中的 mongodb.uri 配置
```

### Q: Agent 无法调用工具

检查以下配置：
1. Tool Guard 是否禁用了该工具
2. MCP 客户端是否正确配置
3. 模型是否支持 function calling

### Q: ReMe Server 无法启动

1. 检查 Python 版本（3.10-3.13）
2. 确保 `.env` 文件中配置了必要的 API Key
3. 检查端口 8085 是否被占用

---

## 🤝 贡献指南

欢迎贡献代码、文档或建议！

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

---

## 📄 许可证

本项目采用 Apache 2.0 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

---

<div align="center">

**TangDynasty** - 让 AI 协作高效有序

[⬆ 返回顶部](#tangdynasty)

</div>
