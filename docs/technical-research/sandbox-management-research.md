# 沙箱管理与可观测性 — 技术调研报告

> 调研日期：2026-07-12
> 调研人：LiangshouX
> 状态：方案设计完成，已实施

---

## 1. 背景与痛点

HiveMind 使用 AgentScope Runtime Java 提供的沙箱系统来为 Agent 提供安全隔离的工具执行环境（Python 代码执行、Shell 命令、文件系统操作、浏览器自动化）。但在实际使用中面临以下痛点：

### 1.1 沙箱运行状态不可见

`TdAgentSandboxManager` 通过懒加载方式创建 `SandboxService`，但**没有任何 API 可以查询沙箱的运行状态**：

- 不知道当前有多少个沙箱容器在运行
- 不知道每个沙箱的端口映射
- 不知道沙箱是否健康
- 不知道沙箱使用的是什么 Docker 镜像

### 1.2 沙箱内部不可观测

沙箱运行后，无法了解沙箱内部发生了什么：

- 沙箱内执行了哪些命令/代码
- 沙箱内有哪些文件产出或残留
- 沙箱的挂载目录 (`mountDir`) 内容是什么
- 沙箱的资源占用情况

### 1.3 工具库页面与沙箱脱节

现有 `/tool-library` 页面只有一个扁平的卡片列表，存在以下问题：

- 不知道每个工具来自哪个沙箱服务
- 不知道沙箱是否正在运行（工具是否实际可用）
- 沙箱类工具（`category: sandbox`）和浏览器工具（`category: browser`）无法与底层沙箱实例关联
- 无法从工具卡片跳转到对应的沙箱详情

### 1.4 无沙箱生命周期管理

- 无法从界面停止正在运行的沙箱
- 无法清理已停止的沙箱容器残留
- 无法批量管理沙箱

---

## 2. 现有架构分析

### 2.1 沙箱在 HiveMind 中的位置

```
TdAgentToolkitFactory.createToolkit()
  │
  ├── TdAgentSandboxManager.getSandboxService()  ← 懒加载，双重检查锁
  │     └── SandboxService(ManagerConfig)         ← AgentScope SDK
  │           └── DockerClientStarter             ← Docker 容器运行时
  │
  ├── BaseSandbox(sandboxService, userId, sessionId)  ← 基础沙箱
  │     ├── RunPythonCodeTool
  │     └── RunShellCommandTool
  │
  ├── [如果 filesystemEnabled]
  │     ├── ReadFileTool, WriteFileTool, ListDirectoryTool, SearchFilesTool
  │
  └── [如果 browserEnabled]
        └── BrowserSandbox(sandboxService, userId, sessionId)
              ├── BrowserNavigateTool, BrowserSnapshotTool
              ├── BrowserClickTool, BrowserTypeTool
              └── BrowserWaitForTool, BrowserTakeScreenshotTool
```

### 2.2 TdAgentSandboxManager 现状

**文件**: `hivemind-agent-engine/src/main/java/com/liangshou/agentic/agents/sandbox/TdAgentSandboxManager.java`

```java
@Component
public class TdAgentSandboxManager {
    private final TdAgentProperties properties;
    private volatile SandboxService sandboxService;

    public SandboxService getSandboxService() {
        if (!properties.getSandbox().isEnabled()) return null;
        // 双重检查锁定，懒初始化
        if (sandboxService == null) {
            synchronized (this) {
                if (sandboxService == null) {
                    SandboxService created = new SandboxService(ManagerConfig.builder().build());
                    created.start();
                    sandboxService = created;
                }
            }
        }
        return sandboxService;
    }

    @PreDestroy
    public void destroy() {
        if (sandboxService != null) sandboxService.close();
    }
}
```

**问题**：
- 只暴露了 `getSandboxService()` 方法，返回原始的 `SandboxService`
- 没有面向业务的查询接口（如：列出所有沙箱、获取沙箱健康状态）
- 没有生命周期管理接口（停止、清理单个沙箱）

### 2.3 工具库页面现状

**文件**: `website/src/pages/Admin/ToolLibrary.tsx`

当前页面结构：
- 顶部：标题 + "同步系统工具" 按钮
- 筛选栏：分类下拉（全部/内置/沙盒/浏览器）+ 搜索框
- 卡片网格：`Row` / `Col` 响应式布局，每个工具一张卡片

每张卡片显示：
- 工具名称 + 启用/禁用状态
- 风险等级标签 + 分类标签
- 描述（2行截断）
- 使用示例（可折叠）
- 控制区：风险等级选择、审批开关、启用开关

**缺失信息**：
- 沙箱类工具不知道来自哪个沙箱
- 无法知道沙箱是否正在运行
- 没有沙箱管理入口

### 2.4 AgentScope SDK 已有能力

`SandboxService`（位于 `.lib-repo/agentscope-runtime-java/sandbox-core/`）已经提供了完整的沙箱管理 API：

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `getAllSandboxes()` | `Map<String, ContainerModel>` | 所有活跃沙箱实例 |
| `getSandboxStatus(containerId)` | `String` | 状态（running/stopped/created 等） |
| `getSandbox(containerId)` | `ContainerModel` | 沙箱详细信息 |
| `startSandbox(containerId)` | `boolean` | 启动沙箱 |
| `stopSandbox(containerId)` | `void` | 停止沙箱 |
| `removeSandbox(containerId)` | `boolean` | 删除沙箱 |
| `cleanupAllSandboxes()` | `void` | 清理所有沙箱 |

`ContainerModel` 包含的字段：

| 字段 | 说明 |
|------|------|
| `containerId` | 容器 ID |
| `containerName` | 容器名称 |
| `baseUrl` | 沙箱 HTTP 端点 |
| `browserUrl` | 浏览器访问 URL |
| `ports` | 端口映射数组 |
| `mountDir` | 宿主机挂载目录 |
| `storagePath` | 存储路径 |
| `version` | Docker 镜像名（可推断沙箱类型） |
| `runtimeToken` | 运行时认证 Token |

`SandboxMap` 接口还提供了：

| 方法 | 说明 |
|------|------|
| `getRefCount(containerId)` | 活跃引用计数 |
| `getTTL(containerId)` | 剩余存活时间 |

**结论**：SDK 层面的能力是完备的，HiveMind 只需要做一层薄包装，将这些能力暴露为 REST API 并在前端展示。

---

## 3. 沙箱类型与工具映射

根据 AgentScope SDK 文档，沙箱有以下类型：

| 沙箱类型 | Docker 镜像 | 提供的工具 | HiveMind 是否使用 |
|---------|-------------|-----------|-----------------|
| **BaseSandbox** | `runtime-sandbox-base` | `run_ipython_cell`, `run_shell_command` | ✅ 核心使用 |
| **FilesystemSandbox** | `runtime-sandbox-filesystem` | 文件读写、目录操作、搜索 | ✅ 通过配置启用 |
| **BrowserSandbox** | `runtime-sandbox-browser` | 浏览器导航、截图、点击、输入 | ✅ 通过配置启用 |
| **GuiSandbox** | `runtime-sandbox-gui` | 桌面 GUI 操作 | ❌ 未使用 |
| **MobileSandbox** | `runtime-sandbox-mobile` | 移动端操作 | ❌ 未使用 |
| **TrainingSandbox** | `runtime-sandbox-appworld/bfcl` | 训练评测 | ❌ 未使用 |
| **AgentBaySandbox** | 云服务 | 同 BaseSandbox | ❌ 未使用 |

**HiveMind 工具注册表** (`SystemToolRegistry`) 中的工具与沙箱对应关系：

| 工具名 | 分类 | 运行环境 | 对应沙箱 |
|--------|------|---------|---------|
| `run_shell_command` | SANDBOX | SANDBOX | BaseSandbox |
| `run_ipython_cell` | SANDBOX | SANDBOX | BaseSandbox |
| `fs_read_file` | SANDBOX | SANDBOX | FilesystemSandbox |
| `fs_write_file` | SANDBOX | SANDBOX | FilesystemSandbox |
| `edit_file` | SANDBOX | SANDBOX | FilesystemSandbox |
| `move_file` | SANDBOX | SANDBOX | FilesystemSandbox |
| `list_directory` | SANDBOX | SANDBOX | FilesystemSandbox |
| `search_files` | SANDBOX | SANDBOX | FilesystemSandbox |
| `browser_navigate` | BROWSER | SANDBOX | BrowserSandbox |
| `browser_snapshot` | BROWSER | SANDBOX | BrowserSandbox |
| `browser_click` | BROWSER | SANDBOX | BrowserSandbox |
| `browser_type` | BROWSER | SANDBOX | BrowserSandbox |
| `browser_wait_for` | BROWSER | SANDBOX | BrowserSandbox |
| `browser_take_screenshot` | BROWSER | SANDBOX | BrowserSandbox |

**注意**：在 HiveMind 的 `TdAgentToolkitFactory.createToolkit()` 中，BaseSandbox 和 BrowserSandbox 是**分别创建**的（BrowserSandbox 独立于 BaseSandbox）。这意味着同一个会话中可能同时存在两个沙箱容器。

---

## 4. 方案设计

### 4.1 架构总览

```
┌─────────────────────────────────────────────────────────┐
│  前端: /tool-library (增强版)                             │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │ 工具卡片列表  │  │ 沙箱服务面板  │  │ 沙箱详情抽屉  │  │
│  │ (现有，增强)  │  │ (NEW)        │  │ (NEW)         │  │
│  └──────┬───────┘  └──────┬───────┘  └───────┬───────┘  │
│         │                 │                   │          │
│         ▼                 ▼                   ▼          │
│  GET /tool-config    GET /sandboxes      GET /sandboxes  │
└─────────────────────────────────────────────────────────┘
           │                       │
           ▼                       ▼
┌─────────────────────────────────────────────────────────┐
│  后端                                                      │
│  ┌─────────────────────┐  ┌────────────────────────────┐ │
│  │ ToolConfigController│  │ SandboxDashboardController │ │
│  │ (现有，不变)         │  │ (NEW)                      │ │
│  └─────────────────────┘  └────────────┬───────────────┘ │
│                                        │                 │
│                          ┌─────────────▼──────────────┐  │
│                          │ SandboxDashboardService    │  │
│                          │ (NEW)                      │  │
│                          └─────────────┬──────────────┘  │
│                                        │                 │
│                          ┌─────────────▼──────────────┐  │
│                          │ TdAgentSandboxManager      │  │
│                          │ (现有，不变)                │  │
│                          └─────────────┬──────────────┘  │
│                                        │                 │
│                          ┌─────────────▼──────────────┐  │
│                          │ SandboxService (SDK)       │  │
│                          │ + InMemorySandboxMap       │  │
│                          └────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### 4.2 后端设计

#### 4.2.1 SandboxDashboardService

**职责**：包装 `TdAgentSandboxManager`，提供面向业务的沙箱查询和管理接口。

```java
@Service
public class SandboxDashboardService {

    private final TdAgentSandboxManager sandboxManager;
    private final TdAgentProperties properties;

    // 查询所有活跃沙箱
    public List<SandboxInfoDTO> listActiveSandboxes() {
        SandboxService service = sandboxManager.getSandboxService();
        if (service == null) return Collections.emptyList();

        Map<String, ContainerModel> all = service.getAllSandboxes();
        return all.entrySet().stream()
            .map(entry -> toInfoDTO(entry.getKey(), entry.getValue(), service))
            .collect(Collectors.toList());
    }

    // 获取健康摘要
    public SandboxHealthDTO getHealth() {
        SandboxService service = sandboxManager.getSandboxService();
        boolean enabled = properties.getSandbox().isEnabled();
        if (!enabled || service == null) {
            return SandboxHealthDTO.builder()
                .sandboxEnabled(false).totalSandboxes(0).build();
        }
        Map<String, ContainerModel> all = service.getAllSandboxes();
        int running = 0, stopped = 0;
        for (String id : all.keySet()) {
            String status = service.getSandboxStatus(id);
            if (isRunning(status)) running++;
            else stopped++;
        }
        return SandboxHealthDTO.builder()
            .sandboxEnabled(true)
            .totalSandboxes(all.size())
            .runningCount(running)
            .stoppedCount(stopped)
            .build();
    }

    // 停止/删除/清理沙箱...
}
```

#### 4.2.2 SandboxInfoDTO

```java
@Data @Builder
public class SandboxInfoDTO {
    private String containerId;       // 容器 ID（短格式）
    private String containerName;     // 容器名称
    private String sandboxType;       // 推断类型: "base" / "browser" / "filesystem"
    private String status;            // "running" / "stopped" / "unknown"
    private String[] ports;           // 端口映射
    private String baseUrl;           // HTTP 端点
    private String browserUrl;        // 浏览器 URL（仅 browser 类型）
    private String mountDir;          // 挂载目录
    private String version;           // Docker 镜像
    private long refCount;            // 活跃引用数
    private List<String> providedTools; // 此沙箱提供的工具名列表
}
```

**沙箱类型推断逻辑**（从 `version` 字段推断）：

```java
private String inferSandboxType(String version) {
    if (version == null) return "unknown";
    if (version.contains("browser")) return "browser";
    if (version.contains("filesystem")) return "filesystem";
    if (version.contains("gui")) return "gui";
    if (version.contains("mobile")) return "mobile";
    return "base";
}
```

#### 4.2.3 SandboxDashboardController

```
GET  /api/agent/sandboxes              → List<SandboxInfoDTO>
GET  /api/agent/sandboxes/health       → SandboxHealthDTO
GET  /api/agent/sandboxes/{id}         → SandboxInfoDTO
POST /api/agent/sandboxes/{id}/stop    → { success: boolean }
POST /api/agent/sandboxes/{id}/remove  → { success: boolean }
POST /api/agent/sandboxes/cleanup      → { removed: int }
```

### 4.3 前端设计

#### 4.3.1 页面布局改造

将现有的单栏卡片网格改为**两栏布局**：

```
┌─────────────────────────────────────────────────────────┐
│  工具库                              [同步系统工具]       │
│  管理智能体可用工具、风险等级和启用状态                      │
├──────────────────────────────┬──────────────────────────┤
│  [全部分类 ▼] [🔍 搜索...]    │  沙箱服务                │
│  共 16 个工具                 │  ┌────────────────────┐  │
│                              │  │ 🟢 运行中: 2       │  │
│  ┌────────┐ ┌────────┐      │  │ 🔴 已停止: 0       │  │
│  │工具卡片 │ │工具卡片 │      │  │ 总计: 2            │  │
│  │🟢 运行中│ │🟢 运行中│      │  └────────────────────┘  │
│  └────────┘ └────────┘      │                          │
│  ┌────────┐ ┌────────┐      │  ┌────────────────────┐  │
│  │工具卡片 │ │工具卡片 │      │  │ BaseSandbox        │  │
│  │🟢 运行中│ │🟢 运行中│      │  │ 🟢 running         │  │
│  └────────┘ └────────┘      │  │ 端口: 49152         │  │
│  ...                        │  │ 引用: 1             │  │
│                              │  │ [详情] [停止]       │  │
│                              │  └────────────────────┘  │
│                              │                          │
│                              │  ┌────────────────────┐  │
│                              │  │ BrowserSandbox     │  │
│                              │  │ 🟢 running         │  │
│                              │  │ 端口: 49153         │  │
│                              │  │ 引用: 1             │  │
│                              │  │ [详情] [停止]       │  │
│                              │  └────────────────────┘  │
│                              │                          │
│                              │  [🧹 清理已停止的沙箱]    │
│                              │  ⏱ 自动刷新: 10s         │
└──────────────────────────────┴──────────────────────────┘
```

#### 4.3.2 工具卡片增强

对 `category === 'sandbox'` 或 `category === 'browser'` 的工具卡片，在分类标签旁增加沙箱运行状态指示器：

```tsx
{/* 在分类标签旁 */}
{isSandboxTool && (
  <Tooltip title={sandboxRunning ? '沙箱运行中' : '沙箱未运行'}>
    <Badge status={sandboxRunning ? 'success' : 'default'} />
  </Tooltip>
)}
```

#### 4.3.3 沙箱详情抽屉

点击沙箱卡片的"详情"按钮，从右侧滑出抽屉：

| 字段 | 展示方式 |
|------|---------|
| 容器 ID | 单行代码块 |
| 容器名称 | 文本 |
| 沙箱类型 | 图标 + 标签 |
| 状态 | Badge（绿色/红色/灰色） |
| Docker 镜像 | 单行代码块（可截断） |
| HTTP 端点 | 可点击链接 |
| 浏览器 URL | 可点击链接（仅 browser 类型） |
| 端口映射 | Tag 列表 |
| 挂载目录 | 单行代码块 |
| 引用计数 | 数字 |
| 提供的工具 | Tag 列表（可点击跳转到对应工具卡片） |
| 操作按钮 | [停止] [删除] |

#### 4.3.4 自动刷新机制

```tsx
const [autoRefresh, setAutoRefresh] = useState(true);

useEffect(() => {
  if (!autoRefresh) return;
  const timer = setInterval(() => {
    loadSandboxes();
    loadHealth();
  }, 10000);
  return () => clearInterval(timer);
}, [autoRefresh]);
```

### 4.4 关键设计决策

| 决策 | 理由 |
|------|------|
| **不新建 MongoDB 集合** | 沙箱是运行时状态，由 AgentScope SDK 内存管理（`InMemorySandboxMap`），不需要持久化 |
| **增强现有页面而非新建** | 工具和沙箱是紧密关联的，放在同一页面减少导航跳转 |
| **轮询而非 WebSocket** | 沙箱状态变化不频繁（通常只在会话开始/结束时变化），10s 轮询足够 |
| **类型从镜像名推断** | `ContainerModel.version` 字段包含镜像名，可从中推断沙箱类型，无需额外字段 |
| **薄包装不侵入 SDK** | 只读取 `SandboxService` 的公开 API，不修改 SDK 代码 |

---

## 5. 影响范围分析

### 5.1 新增文件

| 模块 | 文件 | 说明 |
|------|------|------|
| hivemind-agent-engine | `application/SandboxDashboardService.java` | 沙箱查询服务 |
| hivemind-agent-engine | `application/dto/SandboxInfoDTO.java` | 沙箱信息 DTO |
| hivemind-agent-engine | `application/dto/SandboxHealthDTO.java` | 健康摘要 DTO |
| hivemind-agent-engine | `adapter/controller/SandboxDashboardController.java` | REST 控制器 |

### 5.2 修改文件

| 模块 | 文件 | 变更内容 |
|------|------|---------|
| website | `src/api.ts` | 新增 SandboxInfo / SandboxHealth 类型 + API 方法 |
| website | `src/pages/Admin/ToolLibrary.tsx` | 两栏布局 + 沙箱面板 + 状态指示器 |

### 5.3 不需要修改的文件

| 文件 | 原因 |
|------|------|
| `TdAgentSandboxManager.java` | `getSandboxService()` 已是 public，直接注入使用 |
| `ToolConfigDocument.java` | 工具配置层不变 |
| `ToolConfigController.java` | 工具配置 API 不变 |
| `SystemToolRegistry.java` | 工具定义不变 |
| `SandboxService.java` (SDK) | 作为依赖使用，不修改 |
| `application-agentic.yaml` | 无需新增配置项 |

---

## 6. 验证方案

1. **编译验证**：`mvn clean install` 确保无编译错误
2. **后端 API 验证**：
   - 启动应用 `mvn spring-boot:run -pl hivemind-launcher`
   - 调用 `GET /api/agent/sandboxes/health` → 应返回 `{ sandboxEnabled: true, totalSandboxes: 0, ... }`
   - 通过对话触发 Agent 使用沙箱工具后，再次调用 → 应看到沙箱数量增加
3. **前端验证**：
   - `cd website && npm run dev`，访问 `/tool-library`
   - 沙箱面板应正常渲染，无沙箱时显示"暂无沙箱"
   - 工具卡片功能不受影响
   - 触发沙箱后刷新，沙箱应出现在面板中
4. **TypeScript 验证**：`npm run build` 确保无类型错误

---

## 7. 后续扩展方向

本次方案聚焦于**沙箱运行时可观测性**，以下方向可作为后续迭代：

1. **沙箱日志流**：通过 `SandboxService.callTool()` 在沙箱内执行 `tail -f /var/log/agentscope_runtime.err.log`，实时流式展示沙箱日志
2. **沙箱文件浏览**：通过 `listDirectory()` / `readFile()` 工具查看沙箱内部文件
3. **沙箱终端**：集成 xterm.js，通过 WebSocket 连接到沙箱的 Shell
4. **沙箱资源监控**：通过 Docker API 获取容器的 CPU / 内存 / 网络使用情况
5. **沙箱操作审计**：记录每次沙箱工具调用的输入输出，存储到 MongoDB 供审计
