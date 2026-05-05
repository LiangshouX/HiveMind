# 任务模板库（Edict Library）设计文档

## 需求概述

实现 `/edict-library` 路由对应的任务模板库功能，允许用户：
1. 浏览系统内置任务模板和用户自建模板
2. 填写模板参数后点击"下旨"，调用 Agent 发起任务
3. 下旨后弹出 Modal 提示"旨意已下达"，给出 Session ID，支持跳转到 `/chat/{sessionId}`

---

## 架构设计

### 数据流

```
前端 EdictLibrary.tsx
  ↓ 获取模板列表
后端 EdictTemplateController (REST API)
  ↓ 调用
EdictTemplateService (业务层)
  ↓ 读写
MongoDB / MySQL (存储)
  ↓ 下旨时调用
Agent Engine (TdAgentService)
  ↓ 创建
Session (MongoDB)
  ↓ 返回
sessionId → 前端展示 + 跳转链接
```

---

## 数据库设计

### MongoDB Collection: `edict_templates`

| 字段 | 类型 | 说明 |
|------|------|------|
| `_id` | ObjectId | 主键 |
| `templateId` | String | 模板唯一标识（系统内置：`tpl_xxx`，用户自建：`user_tpl_xxx`） |
| `name` | String | 模板名称 |
| `description` | String | 模板描述 |
| `category` | String | 模板分类（前端筛选用） |
| `icon` | String | 图标 emoji |
| `command` | String | 命令模板（含 `{param}` 占位符） |
| `params` | Array | 参数定义列表（每项含 key, label, type, required, default, options） |
| `depts` | String[] | 承办部门列表 |
| `est` | String | 预估时间 |
| `cost` | String | 预估耗资 |
| `type` | Enum | `SYSTEM`（系统内置） / `USER`（用户自建） |
| `userId` | String | 创建者用户ID（系统内置为空） |
| `createdAt` | Date | 创建时间 |
| `updatedAt` | Date | 更新时间 |

### 索引

- `templateId`: unique
- `type + userId`: 复合索引（查询用户模板）
- `category`: 索引（分类筛选）

---

## 后端实现

### 1. 包路径

```
tang-dynasty-backend/src/main/java/com/liangshou/tangdynasty/backend/
├── edict/
│   ├── adapter/
│   │   └── controller/
│   │       └── EdictTemplateController.java    # REST API
│   ├── application/
│   │   ├── dto/
│   │   │   ├── EdictTemplateDTO.java           # 模板 DTO
│   │   │   ├── EdictTemplateCreateCommand.java # 创建命令
│   │   │   ├── EdictTemplateUpdateCommand.java # 更新命令
│   │   │   └── EdictExecuteCommand.java        # 执行命令
│   │   └── IEdictTemplateService.java          # 服务接口
│   ├── application/impl/
│   │   └── EdictTemplateServiceImpl.java       # 服务实现
│   ├── domain/
│   │   └── model/
│   │       └── EdictTemplateDocument.java      # MongoDB 文档模型
│   └── infrastructure/
│       └── mongo/
│           └── repository/
│               └── EdictTemplateRepository.java # MongoRepository
```

### 2. 复用 agent-engine 中的 Agent 调用

```
tang-dynasty-agent-engine/src/main/java/com/liangshou/tdagent/
├── application/
│   └── IAgentService.java          # 已有接口，用于发起对话/任务
```

---

## REST API 设计

### GET /api/agent/edict-template
**获取模板列表**（系统内置在前，用户自建在后）

Response:
```json
[
  {
    "templateId": "tpl_code_audit",
    "name": "代码审查",
    "description": "对指定代码库进行安全审查",
    "category": "研发效能",
    "icon": "🔍",
    "command": "请审查 {repo} 仓库的 {branch} 分支...",
    "params": [...],
    "depts": ["工部"],
    "est": "2-4小时",
    "cost": "中等",
    "type": "SYSTEM",
    "userId": null,
    "createdAt": "2026-05-05T10:00:00Z",
    "updatedAt": null
  }
]
```

### GET /api/agent/edict-template/{templateId}
**获取单个模板详情**

### POST /api/agent/edict-template
**创建用户自建模板**

Request Body:
```json
{
  "name": "我的模板",
  "description": "描述",
  "category": "分类",
  "icon": "📝",
  "command": "请执行 {task}",
  "params": [{"key": "task", "label": "任务内容", "type": "textarea", "required": true}],
  "depts": ["吏部"],
  "est": "1小时",
  "cost": "低"
}
```

### PUT /api/agent/edict-template/{templateId}
**更新用户自建模板**

### DELETE /api/agent/edict-template/{templateId}
**删除用户自建模板**

### POST /api/agent/edict-template/{templateId}/execute
**下旨（执行模板）**

Request Body:
```json
{
  "params": {
    "repo": "my-repo",
    "branch": "main"
  }
}
```

Response:
```json
{
  "sessionId": "sess_xxxxx",
  "title": "任务标题",
  "message": "旨意已下达"
}
```

---

## 前端实现

### 修改 EdictLibrary.tsx

1. **数据加载**：从后端 API 获取模板列表，替代硬编码的 `TEMPLATES`
2. **模板分类**：系统内置模板 `type === 'SYSTEM'` 排在前面，卡片右上角标注 `【系统内置】` 标签
3. **用户自建模板**：`type === 'USER'` 不标注，支持 CRUD 操作
4. **CRUD UI**：
   - 顶部添加"新建模板"按钮
   - 用户自建模板卡片右上角显示"编辑"和"删除"按钮
5. **下旨流程**：
   - 填写参数 → 点击"下旨"
   - 调用 `POST /api/agent/edict-template/{id}/execute`
   - 成功后弹出 Modal："旨意已下达，Session: {sessionId}"
   - Modal 中提供"查看对话"按钮，跳转到 `/chat/{sessionId}`

---

## 安全设计

- 用户只能 CRUD 自己的模板（通过 `SecurityUtils.getCurrentUserId()` 隔离）
- 系统内置模板不可编辑/删除
- 下旨时自动注入 userId，确保任务归属正确

---

## 实施顺序

1. 创建 MongoDB 文档模型 `EdictTemplateDocument.java`
2. 创建 Repository `EdictTemplateRepository.java`
3. 创建 DTOs `EdictTemplateDTO.java`, `EdictTemplateCreateCommand.java`, `EdictTemplateUpdateCommand.java`, `EdictExecuteCommand.java`
4. 创建 Service 接口和实现 `IEdictTemplateService.java`, `EdictTemplateServiceImpl.java`
5. 创建 Controller `EdictTemplateController.java`
6. 创建系统内置模板数据初始化类
7. 修改前端 `EdictLibrary.tsx` 实现 API 对接
8. 编译验证 `mvn clean compile -pl tang-dynasty-backend -am`

---

## 依赖关系

```
tang-dynasty-backend
  └── depends on tang-dynasty-agent-engine (调用 IAgentService)
```
