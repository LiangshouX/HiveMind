# CoPaw AI Agent 项目详设文档

面向目录：[CoPaw\_main\_src\_copaw\_](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/)

> 说明：该目录是一个“仅包含 `src/copaw` 源码”的抽取版本（未包含完整打包/依赖声明文件）。本文以源码为准，描述其设计架构、关键实现与扩展方式。

***

## 1. 项目目标与运行形态

CoPaw 是一个以 AgentScope/AgentScope Runtime 为基础构建的通用 AI Agent 框架化应用，核心目标是：

- 以 **ReAct** 方式驱动推理与工具调用（shell、文件、浏览器、截图等），并支持动态加载技能（skill）。
- 提供 **多入口形态**：CLI、FastAPI 服务（含 Agent Runtime 的路由）、多渠道消息接入（IM/机器人）。
- 将高风险工具调用纳入 **Tool-Guard 安全审批闭环**：规则检测 → 阻断 → 用户显式批准 → 重新执行。
- 支持 **可选的外部记忆系统**（ReMeLight）与向量/全文检索能力，并在上下文超限时做压缩。
- 支持 **MCP（Model Context Protocol）客户端**热加载，把外部工具端点以“工具集”注入 Agent。

运行形态分两类：

1. CLI（Click）

- 入口：[`__main__.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/__main__.py)、[`cli/main.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/cli/main.py)
- 提供 app/channels/daemon/chats/cron/env/providers/skills 等子命令（具体在 `cli/*_cmd.py`）。

1. FastAPI 服务（含 AgentApp）

- 入口：[`app/_app.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/_app.py)
- 使用 `agentscope_runtime.engine.app.AgentApp` 聚合 runner，并挂载业务 API Router：[`app/routers/__init__.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/routers/__init__.py)

***

## 2. 目录结构与分层边界

源码顶层包：`src/copaw/`（见 [目录](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/)）

推荐按“分层 + 运行时组件”理解：

- **入口层**
  - CLI：[`copaw/cli/`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/cli/)
  - Web 服务：[`copaw/app/_app.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/_app.py)
- **运行时编排层（Runner/Runtime）**
  - 核心 runner：[`app/runner/runner.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/runner/runner.py)
  - 会话持久化：[`app/runner/session.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/runner/session.py)
  - 命令分流（以 `/xxx` 为前缀）：[`app/runner/command_dispatch.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/runner/command_dispatch.py)
- **Agent 核心层**
  - 主 Agent：[`agents/react_agent.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/react_agent.py)
  - Tool-Guard Mixin：[`agents/tool_guard_mixin.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/tool_guard_mixin.py)
  - Prompt 构建：[`agents/prompt.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/prompt.py)
  - 模型工厂：[`agents/model_factory.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/model_factory.py)
  - 工具集合：[`agents/tools/`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/tools/)
  - 技能体系：[`agents/skills_manager.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/skills_manager.py)、[`agents/skills/`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/skills/)
  - 记忆体系：[`agents/memory/memory_manager.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/memory/memory_manager.py)
- **基础设施层（多渠道、MCP、Cron）**
  - Channels：[`app/channels/`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/channels/)（manager/registry/base + 各渠道实现）
  - MCP：[`app/mcp/manager.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/mcp/manager.py)、[`app/mcp/watcher.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/mcp/watcher.py)
  - Cron/Heartbeat：[`app/crons/`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/crons/)
- **配置与持久化**
  - 配置模型：[`config/config.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/config/config.py)
  - 配置读写：[`config/utils.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/config/utils.py)
  - 配置热更新：[`config/watcher.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/config/watcher.py)
  - 环境变量落盘：[`envs/store.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/envs/store.py)
- **安全**
  - Tool Guard 引擎：[`security/tool_guard/engine.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/security/tool_guard/engine.py)
  - 规则样例：[`security/tool_guard/rules/dangerous_shell_commands.yaml`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/security/tool_guard/rules/dangerous_shell_commands.yaml)
  - 审批服务：[`app/approvals/service.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/approvals/service.py)

***

## 3. 关键运行时目录约定（WORKING\_DIR / SECRET\_DIR）

核心常量集中在：[`constant.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/constant.py)

- `WORKING_DIR`：默认 `~/.copaw`（可用 `COPAW_WORKING_DIR` 覆盖）
- `SECRET_DIR`：默认 `~/.copaw.secret`（可用 `COPAW_SECRET_DIR` 覆盖）
- 关键子目录：
  - `active_skills/`：启用技能（Agent 会扫描并注册）
  - `customized_skills/`：用户自定义技能源（同步到 active\_skills）
  - `memory/`：记忆相关数据
  - `custom_channels/`：自定义渠道模块
  - `models/`：本地模型（如 llama.cpp / MLX）相关
- 关键文件：
  - `config.json`：主配置（见 `config/utils.py:get_config_path`）
  - `envs.json`：持久化环境变量（位于 `SECRET_DIR`，见 `envs/store.py`）
  - `jobs.json` / `chats.json`：cron 与 chat 元数据（见 `config/utils.py:get_jobs_path/get_chats_path`）

该设计的意图：

- WORKING\_DIR 放“可读可改、可迁移”的运行配置与资源（技能、日志、会话等）。
- SECRET\_DIR 放密钥/Provider 配置等敏感信息，并尽可能 `chmod 600/700`（见 [`providers/provider_manager.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/providers/provider_manager.py)）。

***

## 4. Agent 核心设计

### 4.1 Agent 基类与 MRO：ReAct + ToolGuardMixin

主类：[`CoPawAgent`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/react_agent.py) 继承关系为：

- `CoPawAgent(ToolGuardMixin, ReActAgent)`

关键点：

- Tool-Guard 通过 MRO 覆写 `_acting/_reasoning` 实现“工具调用前拦截”（详见 [`ToolGuardMixin._acting`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/tool_guard_mixin.py#L103-L182)）。
- Agent 初始化阶段做三件事：
  1. 创建 Toolkit 并注册内置工具（可由 config 开关控制）[`CoPawAgent._create_toolkit`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/react_agent.py#L156-L206)
  2. 扫描/注册 skills（从 WORKING\_DIR 的 `active_skills/`）[`CoPawAgent._register_skills`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/react_agent.py#L208-L232)
  3. 拼装系统提示词（从 WORKING\_DIR 的 markdown 文件集合）[`CoPawAgent._build_sys_prompt`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/react_agent.py#L233-L242)

### 4.2 系统提示词（Prompt）构建策略

实现位于：[`agents/prompt.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/prompt.py)

- 默认读取文件顺序：`AGENTS.md` → `SOUL.md` → `PROFILE.md`（可通过 `config.agents.system_prompt_files` 覆盖）
- 所有文件都“可选”：不存在则跳过，最终兜底为 `DEFAULT_SYS_PROMPT`
- 组装策略：每个文件以 `# 文件名` 分节拼接，利于定位与调试
- Runner 在每次请求都会调用 `agent.rebuild_sys_prompt()`，避免从会话存储里恢复出“过期的 sys\_prompt”：
  - 见 [`AgentRunner.query_handler`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/runner/runner.py#L263-L267)

### 4.3 模型工厂：ProviderManager + Retry + Token Recording

实现位于：[`agents/model_factory.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/model_factory.py)

关键设计：

- `ProviderManager.get_active_chat_model()` 统一返回当前激活的 ChatModel（云端或本地）。
- `create_model_and_formatter()` 会按“真实模型类型”选择 formatter，并做两层包装：
  - `TokenRecordingModelWrapper`：记录 token usage（provider\_id 维度）
  - `RetryChatModel`：对瞬态 API 失败做重试
- 兼容性处理：对 AgentScope 版本差异做 monkey patch（文件 URL → Windows path，避免多模态 block 在 OpenAI 请求里出错）。

Provider 管理实现：[`providers/provider_manager.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/providers/provider_manager.py)

- 内置 provider：OpenAI / Azure OpenAI / DashScope / ModelScope / DeepSeek / MiniMax / Anthropic / Ollama / LM Studio + 本地（llama.cpp、MLX）
- 状态落盘在 `SECRET_DIR/providers/*`，active model 单独持久化（便于 CLI/API 切换后生效）

### 4.4 工具系统：Toolkit + “按配置启停”

Agent 内置工具在 [`react_agent.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/react_agent.py#L181-L205) 以映射表注册：

- shell：`execute_shell_command`
- 文件：`read_file` / `write_file` / `edit_file`
- 浏览器：`browser_use`
- 桌面：`desktop_screenshot`
- 交付：`send_file_to_user`
- 辅助：`get_current_time` / `get_token_usage`

是否启用由 `config.json` 控制（兼容历史：不在 config 中则默认启用）：

- 读取逻辑：[`CoPawAgent._create_toolkit`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/react_agent.py#L172-L205)

### 4.5 Skills 体系：三层目录 + 同步策略

核心实现：[`agents/skills_manager.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/skills_manager.py)

- builtin skills：代码内置（`copaw/agents/skills/*`）
- customized skills：`WORKING_DIR/customized_skills/*`（用户自定义）
- active skills：`WORKING_DIR/active_skills/*`（最终启用、被 Agent 注册的目录）

同步策略（核心思想）：

- 将 builtin + customized 合并后同步到 active\_skills（customized 同名优先）
- 同步时忽略运行时产物（`__pycache__`、`.pyc`、`Thumbs.db` 等）

Agent 注册：遍历 `active_skills` 下有效 skill 目录，调用 `toolkit.register_agent_skill(path)`。

### 4.6 MCP 集成：热加载的外部工具端点

实现位于：[`app/mcp/manager.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/mcp/manager.py)

- 支持 `stdio` 与 `http` 两类 transport（分别对应 `StdIOStatefulClient` 与 `HttpStatefulClient`）
- 关键目标：**热替换**（replace\_client）采用“先连新 → 再 swap + close old”的策略，减少锁持有时间并避免长时间阻塞
- Runner 每次请求都会 `await mcp_manager.get_clients()` 获取当前生效 clients，并在 Agent 构造后调用：
  - [`CoPawAgent.register_mcp_clients`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/react_agent.py#L322-L379)

### 4.7 记忆管理（MemoryManager）：ReMeLight + 可选启用

实现位于：[`agents/memory/memory_manager.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/memory/memory_manager.py)

- 依赖 `reme.reme_light.ReMeLight`（缺少依赖会直接抛错）
- 向量检索启用条件：同时配置 `EMBEDDING_API_KEY` 与 `EMBEDDING_MODEL_NAME`
- 后端选择：`MEMORY_STORE_BACKEND=auto` 时在 Windows 默认 `local`，其他平台默认 `chroma`
- Agent 侧启用后会：
  - 将 `self.memory` 替换为 memory\_manager 的 in-memory 实现
  - 注册 `memory_search` 工具（便于 LLM 在对话中检索）
  - 见 [`CoPawAgent._setup_memory_manager`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/react_agent.py#L244-L278)

此外，记忆压缩通过 hook 触发（见 4.9）。

### 4.8 Tool-Guard 安全体系：deny/guard/approve

核心组件：

- 引擎与 guardian 编排：[`security/tool_guard/engine.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/security/tool_guard/engine.py)
- Agent 侧拦截：[`agents/tool_guard_mixin.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/tool_guard_mixin.py)
- 审批存储：[`app/approvals/service.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/approvals/service.py)
- 规则样例：[`dangerous_shell_commands.yaml`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/security/tool_guard/rules/dangerous_shell_commands.yaml)

关键机制：

1. denied\_tools（无条件禁止）

- 当 tool 位于 denied 集合：直接阻断，不提供审批入口（见 [`ToolGuardMixin._acting_auto_denied`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/tool_guard_mixin.py#L187-L236)）。

1. guarded\_tools（需要检查/可能审批）

- 对 guarded scope 内的工具，执行 `ToolGuardEngine.guard(tool_name, params)`。
- 若发现命中（findings 非空），进入审批流程：写入 pending，并返回 tool\_result，提示用户在会话中执行 `/approve`（或 `/daemon approve`）以继续。

1. 审批的一次性消费与参数一致性校验

- `ApprovalService.consume_approval` 会对“已批准的 tool call 参数”做比对，防止批准 `rm a.txt` 被复用为 `rm -rf /`：
  - 见 [`ApprovalService.consume_approval`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/approvals/service.py#L158-L204)

1. Runner 端审批优先处理

- Runner 每次收到消息，先检查 session 是否存在 pending approval：
  - 若用户输入 `/approve`：消费并允许消息继续进入 Agent，让 LLM “重新发起工具调用”
  - 若用户输入其它：默认拒绝，并清理会话里的 denial mark（避免污染记忆）
  - 见 [`AgentRunner._resolve_pending_approval`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/runner/runner.py#L65-L142)、[`AgentRunner.query_handler`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/runner/runner.py#L143-L174)

### 4.9 Hook 体系：Bootstrap 与 Memory Compaction

见 [`CoPawAgent._register_hooks`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/react_agent.py#L279-L305)

- BootstrapHook：检测 WORKING\_DIR 是否存在 `BOOTSTRAP.md`，若存在则进入“引导模式”（提示用户完成初次配置）。引导文案由 [`prompt.build_bootstrap_guidance`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/prompt.py#L174-L223) 生成。
- MemoryCompactionHook：当启用 MemoryManager 时，在 pre\_reasoning 时机自动压缩（避免上下文溢出）。

***

## 5. Runner 与一次请求的全链路

### 5.1 服务装配（lifespan）

服务启动过程在 [`app/_app.py:lifespan`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/_app.py#L62-L149) 完成，核心步骤：

1. `runner.start()`
2. 初始化 MCPClientManager（从 config 加载）并注入 runner
3. 根据 config 初始化 ChannelManager 并 start\_all
4. 初始化 CronManager 并启动（含 heartbeat）
5. 初始化 ChatManager（`chats.json` repo），注入 runner
6. 启动 ConfigWatcher（channel/heartbeat 热更新）
7. 启动 MCPConfigWatcher（MCP clients 热更新）
8. 将 ChannelManager 注入 ApprovalService，用于主动推送审批提示（部分渠道）

### 5.2 query\_handler 主流程

核心实现在 [`AgentRunner.query_handler`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/runner/runner.py#L143-L309)：

1. 取最后一条用户文本 → `query`
2. 优先处理 pending approval（approve/deny/timeout）
3. 若是命令（`/xxx`），走命令分流 `run_command_path(...)`（daemon/cron 等）
4. 构造 `env_context`（包含 session/user/channel/working\_dir 等）
5. 获取 MCP clients（热加载快照）
6. 读取 config：`max_iters`、`max_input_length`
7. 构造 Agent：`CoPawAgent(..., mcp_clients=..., memory_manager=..., request_context=...)`
8. `agent.register_mcp_clients()` 注入 MCP 工具
9. `session.load_session_state(...)` 恢复会话记忆
10. `agent.rebuild_sys_prompt()` 以磁盘最新 prompt 覆盖会话中旧 prompt
11. 运行并流式输出：`stream_printing_messages(agents=[agent], coroutine_task=agent(msgs))`
12. finally：`save_session_state(...)` 持久化会话；更新 Chat 元数据

***

## 6. 多渠道接入（Channels）关键实现

核心管理器：[`app/channels/manager.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/channels/manager.py)

设计要点：

- **每个 channel 一个队列**，默认队列上限 `_CHANNEL_QUEUE_MAXSIZE=1000`
- **每个 channel 多 worker 并发**（默认 `_CONSUMER_WORKERS_PER_CHANNEL=4`），提升吞吐
- **同 session 的 debounce 合并**：
  - 通过 `BaseChannel.get_debounce_key(payload)` 标识会话
  - 若某 session 正在处理，新 payload 暂存至 `_pending`，worker 完成后再合并回队列，避免同一会话并发打散导致上下文错乱
- 支持从 config 生成各 channel 实例：`ChannelManager.from_config(...)`，并支持 plugin channel（extra keys）

注册表与动态发现：

- registry：[`app/channels/registry.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/channels/registry.py)
- 各渠道实现位于 `app/channels/<channel_name>/channel.py`

***

## 7. 配置体系与热更新

### 7.1 配置模型与落盘文件

配置模型（Pydantic）定义在：[`config/config.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/config/config.py)

配置读写：

- `config.json` 默认路径：[`config/utils.py:get_config_path`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/config/utils.py#L321-L324)
- 加载：[`config/utils.py:load_config`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/config/utils.py#L331-L347)
- 保存：[`config/utils.py:save_config`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/config/utils.py#L349-L360)

常见配置影响面：

- `agents.running.*`：迭代次数、上下文窗口、压缩阈值
- `tools.builtin_tools.*.enabled`：内置工具开关
- `channels.*`：各渠道连接参数、策略（dm/group allowlist 等）
- `mcp.clients.*`：MCP 客户端定义（transport/url/stdio command/env/cwd）
- `security.tool_guard.*`：tool guard 的启用与范围

### 7.2 ConfigWatcher：基于 mtime 的轮询热更新

实现：[`config/watcher.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/config/watcher.py)

- 每 2 秒轮询 `config.json` 的 mtime
- 若 channels 部分发生变化，仅 reload 发生差异的 channel（clone + replace）
- 若 heartbeat 配置变化，调用 cron\_manager.reschedule\_heartbeat()

### 7.3 MCPConfigWatcher：MCP clients 热更新

实现：[`app/mcp/watcher.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/mcp/watcher.py)

- 监听同一份 `config.json`，在 `mcp.clients` 变更时触发 `replace_client/remove_client`
- 结合 MCPClientManager 的“先连新再切换”策略实现无重启更新

***

## 8. 环境变量持久化（envs.json）与启动注入

实现：[`envs/store.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/envs/store.py)

- envs.json 默认位置：`SECRET_DIR/envs.json`
- `save_envs` 会写文件并同步到 `os.environ`
- `load_envs_into_environ` 在服务模块 import 时调用（见 [`app/_app.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/_app.py#L49-L52)），保证 lifespan 启动前环境已就绪
- 注入策略：保护关键敏感 key，不覆盖当前进程已显式设置的环境变量

***

## 9. 关键时序（文字版）

### 9.1 用户消息 → Agent 执行（含 Tool-Guard）

1. Channel/HTTP 将消息变为 AgentRequest → 交给 Runner
2. Runner 先检查 `ApprovalService` 是否有 pending
3. 如无审批待处理：
   - 若输入以 `/` 开头，进入命令分流（daemon/cron 等）
   - 否则构造 CoPawAgent，恢复会话，重建 sys prompt
4. Agent 推理到工具调用：
   - ToolGuardMixin 在 `_acting` 前拦截
   - 若 denied：直接返回阻断 tool\_result
   - 若 guarded 且命中规则：写入 pending，返回“需要批准”的 tool\_result
5. 用户输入 `/approve`：
   - Runner 消费审批并让消息继续进入 Agent
   - ToolGuardMixin 在下一次同 tool\_name + 同参数的调用前会识别“已批准”，跳过 guard 检查并真正执行工具

### 9.2 config.json 变更 → 在线热更新

1. ConfigWatcher 轮询到 mtime 变化
2. diff channels：仅对变化 channel 做 replace
3. diff heartbeat：触发 reschedule
4. MCPConfigWatcher diff mcp clients：replace/remove 对应 client

***

## 10. 二次开发与扩展点（建议路径）

### 10.1 新增内置工具（Tool）

建议做法：

- 在 [`agents/tools/`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/tools/) 新增工具函数模块
- 在 [`CoPawAgent._create_toolkit`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/react_agent.py#L181-L205) 增加映射项，并在 config schema（`tools.builtin_tools`）加入开关（若项目已有对应 schema）
- 若工具涉及写文件/执行命令，建议加入 Tool-Guard 规则与 deny/guard 范围配置

### 10.2 新增技能（Skill）

两种方式：

1. 作为 builtin skill：放到 `copaw/agents/skills/<skill_name>/`，至少包含 `SKILL.md`
2. 作为 customized skill：写入 `WORKING_DIR/customized_skills/<skill_name>/`，再同步到 `active_skills`

技能系统的目录树表示、同步与覆盖策略见 [`skills_manager.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/skills_manager.py)。

### 10.3 新增渠道（Channel）

- 实现一个 `BaseChannel` 子类，放到 `app/channels/<name>/channel.py`
- 在 registry 注册，或通过 `WORKING_DIR/custom_channels` 动态加载（具体看 [`channels/registry.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/channels/registry.py) 的发现逻辑）
- 确保实现 `get_debounce_key/merge_requests` 等契约，避免同会话并发乱序

### 10.4 接入外部工具生态（MCP）

- 在 `config.json` 添加/更新 `mcp.clients.*`
- MCPClientManager 自动建立连接并在每次请求注入 Agent
- 推荐对高风险 MCP 工具同样纳入 Tool-Guard（如其最终会调用 shell/file）

***

## 11. 风险点与改进建议（面向工程化）

- 依赖缺失风险：MemoryManager 强依赖 `reme`，缺少会抛 RuntimeError（可考虑把 memory\_manager 的初始化变为更软的“可选功能”，并在 UI/CLI 提示安装依赖）。
- 规则覆盖面：`dangerous_shell_commands.yaml` 以 `\brm\b`/`\bmv\b` 作为粗粒度拦截，能保证安全但可能引起误报（可按路径白名单/只拦截危险参数组合做细化）。
- 热更新一致性：channels/mcp 热更新是在线替换，建议在更大规模使用时补充“变更审计/回滚信息”记录，以便定位线上问题。
- 多渠道并发：ChannelManager 已做 per-session debounce 与 pending 合并，但若某 channel 的 `get_debounce_key` 粒度过粗可能造成不同会话被错误合并；开发新增 channel 时需重点验证。

***

## 12. 快速阅读索引（从哪里看起）

- 服务启动与组件装配：[`app/_app.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/_app.py)
- 单次请求的主流程：[`app/runner/runner.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/runner/runner.py)
- Agent 行为（工具/技能/记忆/Hook）：[`agents/react_agent.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/react_agent.py)
- Tool-Guard 审批闭环：[`agents/tool_guard_mixin.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/agents/tool_guard_mixin.py)、[`security/tool_guard/engine.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/security/tool_guard/engine.py)、[`app/approvals/service.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/approvals/service.py)
- 配置 schema 与热更新：[`config/config.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/config/config.py)、[`config/watcher.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/config/watcher.py)
- MCP 热加载：[`app/mcp/manager.py`](file:///d:/Code/Java/TangDynasty/example/CoPaw_main_src_copaw_/src/copaw/app/mcp/manager.py)

