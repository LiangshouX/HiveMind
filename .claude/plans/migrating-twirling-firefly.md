# 全面重构计划：移除三省六部主题，转为通用云端AI助理平台

## Context

项目当前以唐王朝"三省六部"为主题，所有命名（类名、表名、API路径、前端路由、UI文案）都带有浓厚的古风色彩。用户希望将其转变为一个类似 OpenClaw 的通用云端 AI 助理平台，彻底去除唐王朝主题。

## 命名映射表

### 1. 核心概念映射

| 原名（唐王朝） | 新名（通用AI助理） | 说明 |
|---|---|---|
| 三省六部 | 多Agent协作流水线 | 架构概念 |
| 宰相 / ZDXL | Triage Agent / `AGENT_TRIAGE` | 消息分拣 |
| 中书令 / VSUU | Planner Agent / `AGENT_PLANNER` | 任务规划 |
| 门下省 / MFXX | Reviewer Agent / `AGENT_REVIEWER` | 方案审查 |
| 尚书令 / UHUU | Executor Agent / `AGENT_EXECUTOR` | 执行派发 |
| 皇上 | 用户 (User) | 终端用户 |
| 太子 | 助理 (Assistant) | AI助手 |
| 旨意 | 任务 (Task) | 用户下达的任务 |
| 圣旨 | 模板 (Template) | 预设任务模板 |
| 奏折 | 报告 (Report) | 任务执行报告/审批 |
| 回奏 | 响应 (Response) | 执行结果回传 |
| 早朝 | 工作台 (Workspace) | 主控制台 |
| 御书房 | 任务中心 (Task Center) | 任务管理 |
| 御史台 | 管理 (Admin) | 系统管理 |
| 九司 | 服务 (Services) | 外部服务集成 |
| 吏部/户部/礼部/兵部/刑部/工部 | 具体功能模块名（如数据部、文档部等） | 子Agent/部门 |

### 2. Java 类名映射（backend 模块 Edict 域）

| 原类名 | 新类名 |
|---|---|
| `EdictTemplate*` | `TaskTemplate*` |
| `EdictTasks*` | `AgentTask*` |
| `EdictMemorial*` | `TaskReport*` |
| `EdictTaskFlowLog*` | `TaskFlowLog*` |
| `EdictExecuteCommand` | `TaskExecuteCommand` |
| `SystemEdictTemplateInitializer` | `SystemTaskTemplateInitializer` |

涉及文件约 40 个（controller, service, impl, dto, vo, po, mapper, support, support/impl, test）。

### 3. 数据库映射

| 原名 | 新名 |
|---|---|
| 数据库 `tang_dynasty` | **保持不变** |
| 表 `edict_tasks` | `agent_tasks` |
| 表 `edict_memorial` | `task_reports` |
| 表 `edict_task_flow_log` | `task_flow_logs` |
| MongoDB `edict_templates` | `task_templates` |
| 列 `official_id` | `agent_id` |
| 状态 `ZDXL/VSUU/MFXX` | `AGENT_TRIAGE/AGENT_PLANNER/AGENT_REVIEWER/ASSIGNED/DOING/PREVIEW/DONE/FINISH` |

### 4. API 路径映射

| 原路径 | 新路径 |
|---|---|
| `/api/agent/edict-template` | `/api/agent/task-template` |
| `/api/edict-taskss` | `/api/agent-tasks` |
| `/api/edict-memorials` | `/api/task-reports` |
| `/api/edict-task-flow-logs` | `/api/task-flow-logs` |

### 5. 前端目录和路由映射

| 原目录/路由 | 新目录/路由 |
|---|---|
| `MorningCourt/` | `Workspace/` |
| `ImperialStudy/` | `TaskCenter/` |
| `Censorate/` | `Admin/` |
| `/morning-court` | `/workspace` |
| `/imperial-study` | `/task-center` |
| `/censorate` | `/admin` |
| `/edict-library` | `/task-templates` |
| `/edict-board` | `/task-board` |
| `/memorials` | `/reports` |

### 6. 前端组件/文件映射

| 原名 | 新名 |
|---|---|
| `EdictLibrary.tsx` | `TaskTemplateLibrary.tsx` |
| `EdictBoard.tsx` | `TaskBoard.tsx` |
| `Memorials.tsx` | `Reports.tsx` |
| `ImperialSendButton.tsx` | `ChatSendButton.tsx` |
| `OfficialManagement.tsx` | `AgentManagement.tsx` |

### 7. Agent 角色 prompt 引用

`souls/` 目录在仓库中不存在（可能是运行时生成或外部挂载），只需更新 Java 代码中的引用：
- `SoulPromptConstants.java`: `ZDXL_BUILTIN_SOUL` → `AGENT_TRIAGE_BUILTIN_SOUL`，`loadSoul("ZDXL")` → `loadSoul("AGENT_TRIAGE")`
- `SoulPromptLoader.java`: 注释中的 ZDXL/VSUU/MFXX/UHUU 引用
- `ProfilePromptBuilder.java`: 注释中的 "三省六部 AI 助手" 引用

### 8. 包名

- `com.liangshou.tangdynasty.agentic` → `com.liangshou.agentic`（去除 `tangdynasty`）
- 整个 agent-engine 模块的所有 Java 文件包名都需要变更
- `@MapperScan`、`@ComponentScan`、YAML logger 配置同步更新

### 9. Maven 构建产物（用户决定：保持不变）

- **目录名保持不变**: `tang-dynasty-launcher/`、`tang-dynasty-backend/`、`tang-dynasty-agent-engine/`
- **artifactId 保持不变**: 与目录名保持一致
- **数据库名保持不变**: `tang_dynasty`
- 只改内部代码引用、配置和 UI 文案

## 执行顺序

分 8 个阶段，按依赖关系排序：

### Phase 1: 包名重命名（agent-engine）
- 将 `com.liangshou.tangdynasty.agentic` → `com.liangshou.agentic`
- 涉及 agent-engine 模块所有 Java 文件（~60+ 文件）
- 更新 `@MapperScan`、`@ComponentScan`、YAML logger 配置
- 移动目录: `tangdynasty/agentic/` → `agentic/`

### Phase 2: Java 类名重命名（backend Edict 域）
- 重命名所有 `Edict*` 类为 `Task*` / `TaskReport*` / `TaskTemplate*` / `AgentTask*`
- 更新所有 import、引用、@TableName、@Document 注解
- 更新 API 路径 (@RequestMapping)

### Phase 3: Agent 角色重命名
- 重命名 `soul/` 目录: ZDXL→AGENT_TRIAGE, VSUU→AGENT_PLANNER, MFXX→AGENT_REVIEWER, UHUU→AGENT_EXECUTOR
- 更新 `SoulPromptLoader.java`、`SoulPromptConstants.java`
- 更新 `ProfilePromptBuilder.java` 中的中文引用
- 更新 SQL 中的状态值注释

### Phase 4: 数据库和配置
- SQL 脚本: 表名（edict_tasks→agent_tasks 等）、列名（official_id→agent_id）、注释
- YAML 配置: 应用名、JWT secret（去除 TangDynasty 字样）
- 数据库名 `tang_dynasty` 保持不变

### Phase 5: 前端目录和文件重命名
- 移动目录: MorningCourt→Workspace, ImperialStudy→TaskCenter, Censorate→Admin
- 重命名组件文件
- 更新所有 import 路径

### Phase 6: 前端内容和 UI 文案
- `store.ts`: PIPE 定义、部门映射、状态标签
- `api.ts`: 接口名和函数名
- `MainLayout.tsx`: 导航标签、品牌名
- `App.tsx`: 路由路径
- 各页面组件中的中文文案
- CSS 类名 (.imperial-heading → .chat-heading)

### Phase 7: 文档更新
- `CLAUDE.md`: 完全重写项目描述
- `README.md`: 完全重写，去除所有唐王朝元素
- `docs/` 下的 Design.md 文件
- `docs/ARCHITECTURE.md`

### Phase 8: 构建配置（保持不变）
- pom.xml artifactId: 保持 `tang-dynasty-*`
- Dockerfile: 保持不变
- CI/CD: 保持不变
- 目录名、artifactId、数据库名均不改动

## 验证

每个阶段完成后：
1. `mvn clean compile` — 确保 Java 编译通过
2. 检查前端 TypeScript 编译（如有 Node.js 环境）
3. grep 确认旧名称不再出现

最终验证：
- `mvn clean install` 全量构建
- grep -r "三省六部\|宰相\|中书令\|门下省\|尚书令\|皇上\|旨意\|奏折\|圣旨\|御书房\|御史台\|早朝\|ZDXL\|VSUU\|MFXX\|UHUU\|Edict" 确认无遗漏（排除数据库名 `tang_dynasty` 和目录名 `tang-dynasty-*`）
