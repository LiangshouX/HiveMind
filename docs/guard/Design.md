# 工具管理配置设计文档

## 1. 背景与目标

### 1.1 现状问题

当前系统中工具的风险等级、启用状态等配置**硬编码**在以下位置：

- **ToolGuardEngine.java**：`APPROVAL_TOOLS` 列表硬编码需要审批的工具
- **ToolGuardEngine.java**：`SHELL_DENY_PATTERNS` 和 `SENSITIVE_PATH_PATTERNS` 硬编码危险命令
- **TdAgentToolkitFactory.java**：工具注册逻辑写死在代码中
- **前端 ToolLibrary.tsx**：使用 mock 数据，无真实工具列表

这导致：
1. **无法动态调整**工具风险等级，需要修改代码并重启
2. **无法按用户隔离**工具启用状态（某些用户可能不需要代码执行工具）
3. **无法查看工具详情**（用途、示例、运行环境）
4. **缺乏统一管理入口**，工具配置分散在代码各处

### 1.2 设计目标

构建一个**可配置、用户隔离、持久化**的工具管理系统：

✅ **工具库查看**：前端 `/tool-library` 展示所有可用工具的详细信息  
✅ **风险等级调整**：支持用户自定义修改工具风险等级（LOW/MEDIUM/HIGH/CRITICAL）  
✅ **启用/禁用控制**：支持按用户开关工具，禁用工具对 Agent 不可见  
✅ **用户隔离**：工具配置按 `userId` 隔离，存储到 MongoDB  
✅ **同步系统工具**：提供"同步"按钮，从系统默认配置同步新增的工具  
✅ **运行时生效**：ToolGuardEngine 和 TdAgentToolkitFactory 读取用户配置动态决策  

---

## 2. 数据模型设计

### 2.1 MongoDB 集合：`tool_configs`

存储用户级别的工具配置，继承系统默认配置。

```json
{
  "_id": "uuid-string",
  "userId": "user-123",
  "toolName": "run_shell_command",
  "riskLevel": "HIGH",
  "enabled": true,
  "description": "执行 Shell 命令，用于运行脚本、安装依赖等",
  "category": "sandbox",
  "runEnvironment": "sandbox",
  "examples": [
    {
      "title": "安装 Python 包",
      "input": "{\"command\": \"pip install requests\"}"
    }
  ],
  "approvalRequired": true,
  "denyPatterns": ["rm -rf", "del /f", "format "],
  "customized": true,
  "createdAt": "2026-05-05T10:00:00Z",
  "updatedAt": "2026-05-05T12:00:00Z"
}
```

### 2.2 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `_id` | String | ✅ | UUID 主键 |
| `userId` | String | ✅ | 用户唯一标识 |
| `toolName` | String | ✅ | 工具名称（唯一索引） |
| `riskLevel` | String | ✅ | 风险等级：LOW/MEDIUM/HIGH/CRITICAL |
| `enabled` | Boolean | ✅ | 是否启用 |
| `description` | String | ✅ | 工具用途描述 |
| `category` | String | ✅ | 工具分类：`builtin`（内置）/ `sandbox`（沙盒）/ `browser`（浏览器）/ `custom`（自定义） |
| `runEnvironment` | String | ✅ | 运行环境：`system`（系统）/ `sandbox`（沙盒隔离） |
| `examples` | Array | ❌ | 使用示例列表 |
| `approvalRequired` | Boolean | ✅ | 是否需要人工审批 |
| `denyPatterns` | Array | ❌ | 拒绝执行的命令模式列表 |
| `customized` | Boolean | ✅ | 是否用户自定义（false=系统同步） |
| `createdAt` | Instant | ✅ | 创建时间 |
| `updatedAt` | Instant | ✅ | 更新时间 |

### 2.3 索引设计

```javascript
// 唯一索引：同一用户下工具名称唯一
db.tool_configs.createIndex({userId: 1, toolName: 1}, {unique: true})

// 查询索引：按用户查询所有工具
db.tool_configs.createIndex({userId: 1, enabled: 1})

// 分类查询索引
db.tool_configs.createIndex({userId: 1, category: 1})
```

---

## 3. 系统默认工具清单

### 3.1 内置工具（builtin）

| 工具名称 | 描述 | 风险等级 | 启用 | 环境 |
|----------|------|----------|------|------|
| `get_session_id` | 获取当前会话 ID | LOW | ✅ | system |
| `get_history_preview` | 获取历史对话预览 | LOW | ✅ | system |
| `search_memory` | 搜索长期记忆 | LOW | ✅ | system |

### 3.2 沙盒工具（sandbox）

| 工具名称 | 描述 | 风险等级 | 启用 | 审批 | 拒绝模式 |
|----------|------|----------|------|------|----------|
| `run_shell_command` | 执行 Shell 命令 | HIGH | ✅ | ✅ | `rm -rf`, `del /f`, `format `, `shutdown `, `reboot ` |
| `run_ipython_cell` | 执行 Python 代码 | HIGH | ✅ | ✅ | `os.system`, `subprocess`, `__import__` |
| `fs_read_file` | 读取文件内容 | LOW | ✅ | ❌ | - |
| `fs_write_file` | 写入文件内容 | HIGH | ✅ | ✅ | `/etc/passwd`, `/etc/shadow` |
| `edit_file` | 编辑文件（查找替换） | HIGH | ✅ | ✅ | - |
| `move_file` | 移动/重命名文件 | MEDIUM | ✅ | ✅ | - |
| `list_directory` | 列出目录内容 | LOW | ✅ | ❌ | - |
| `search_files` | 搜索文件 | LOW | ✅ | ❌ | - |

### 3.3 浏览器工具（browser）

| 工具名称 | 描述 | 风险等级 | 启用 | 审批 |
|----------|------|----------|------|------|
| `browser_navigate` | 导航到网页 | HIGH | ✅ | ✅ |
| `browser_snapshot` | 获取网页快照 | LOW | ✅ | ❌ |
| `browser_click` | 点击网页元素 | HIGH | ✅ | ✅ |
| `browser_type` | 输入文本到网页 | HIGH | ✅ | ✅ |
| `browser_wait_for` | 等待页面元素 | LOW | ✅ | ❌ |
| `browser_take_screenshot` | 截取网页截图 | LOW | ✅ | ❌ |

---

## 4. 架构设计

### 4.1 分层架构

```
┌─────────────────────────────────────────────┐
│  前端 (website)                              │
│  /tool-library → ToolLibrary.tsx            │
│  - 工具列表展示                              │
│  - 风险等级修改                              │
│  - 启用/禁用开关                             │
│  - 同步系统工具按钮                          │
└──────────────┬──────────────────────────────┘
               │ HTTP API
┌──────────────▼──────────────────────────────┐
│  Controller 层                               │
│  ToolConfigController                        │
│  - GET    /api/agent/tool-config            │
│  - PUT    /api/agent/tool-config/{toolName}  │
│  - POST   /api/agent/tool-config/sync        │
└──────────────┬──────────────────────────────┘
               │
┌──────────────▼──────────────────────────────┐
│  Application 层                              │
│  IToolConfigService                          │
│  - listByUserId()                            │
│  - updateToolConfig()                        │
│  - syncSystemTools()                         │
│  - getEffectiveConfig(toolName, userId)      │
└──────────────┬──────────────────────────────┘
               │
┌──────────────▼──────────────────────────────┐
│  Domain 层                                   │
│  ToolConfigDocument (MongoDB 文档)           │
│  ToolConfigRepository (Spring Data Mongo)    │
│  ToolConfigDTO / ToolConfigUpdateCommand     │
└──────────────┬──────────────────────────────┘
               │
┌──────────────▼──────────────────────────────┐
│  Infrastructure 层                           │
│  ToolConfigProvider (读取用户配置)           │
│  SystemToolRegistry (系统默认工具注册表)     │
└──────────────┬──────────────────────────────┘
               │
┌──────────────▼──────────────────────────────┐
│  集成点                                      │
│  ToolGuardEngine (读取风险等级和审批规则)    │
│  TdAgentToolkitFactory (读取启用状态)        │
└─────────────────────────────────────────────┘
```

### 4.2 核心流程

#### 4.2.1 工具配置查询流程

```
前端请求 /api/agent/tool-config
    ↓
ToolConfigController.listByUserId(userId)
    ↓
IToolConfigService.listByUserId(userId)
    ↓
1. 查询 MongoDB tool_configs WHERE userId = ?
2. 如果结果为空（新用户），调用 syncSystemTools() 同步系统默认配置
3. 返回完整工具列表（含用户自定义 + 系统默认）
    ↓
返回 JSON 数组
```

#### 4.2.2 工具配置更新流程

```
前端 PUT /api/agent/tool-config/run_shell_command
  Body: {riskLevel: "CRITICAL", enabled: false}
    ↓
ToolConfigController.updateToolConfig(toolName, command)
    ↓
IToolConfigService.updateToolConfig(userId, toolName, command)
    ↓
1. 验证 riskLevel 枚举值
2. 查询现有配置
3. 如果不存在，先创建系统默认配置副本
4. 更新风险等级/启用状态
5. 标记 customized = true
6. 保存到 MongoDB
    ↓
返回更新后的配置
```

#### 4.2.3 同步系统工具流程

```
前端 POST /api/agent/tool-config/sync
    ↓
ToolConfigController.syncSystemTools(userId)
    ↓
IToolConfigService.syncSystemTools(userId)
    ↓
1. 获取系统注册表中的所有工具（SystemToolRegistry）
2. 查询用户已有的工具配置
3. 对比差异，找出新增的工具
4. 为每个新增工具创建用户配置（继承系统默认值）
5. 批量保存到 MongoDB
    ↓
返回同步的工具数量
```

#### 4.2.4 ToolGuardEngine 集成流程

```
Agent 调用工具: run_shell_command
    ↓
GuardedAgentTool.callAsync()
    ↓
ToolGuardEngine.evaluate(toolName, input)
    ↓
1. 从 ToolConfigProvider 获取用户对该工具的配置
2. 检查 enabled 状态 → 如果禁用，直接拒绝
3. 读取 riskLevel 决定风险等级
4. 读取 approvalRequired 决定是否需要审批
5. 检查 denyPatterns 是否匹配拒绝模式
6. 返回 ToolGuardDecision
    ↓
如果 requiresApproval → 抛出 ToolSuspendException
如果 !allowed → 返回错误结果
如果 allowed → 继续执行
```

#### 4.2.5 TdAgentToolkitFactory 集成流程

```
createToolkit(context)
    ↓
1. 注册内置工具（始终启用）
2. 如果沙箱启用：
   a. 遍历系统工具注册表
   b. 对每个工具调用 ToolConfigProvider.getEffectiveConfig(toolName, userId)
   c. 如果 enabled = false，跳过注册
   d. 如果 enabled = true，注册并应用 Tool Guard
    ↓
返回 Toolkit
```

---

## 5. 技术实现细节

### 5.1 MongoDB 文档模型

```java
@Document(collection = "tool_configs")
public class ToolConfigDocument {
    @Id
    private String id;
    
    private String userId;
    private String toolName;
    private ToolRiskLevel riskLevel;
    private Boolean enabled;
    private String description;
    private ToolCategory category;  // enum: BUILTIN, SANDBOX, BROWSER, CUSTOM
    private RunEnvironment runEnvironment;  // enum: SYSTEM, SANDBOX
    private List<ToolExample> examples;
    private Boolean approvalRequired;
    private List<String> denyPatterns;
    private Boolean customized;
    private Instant createdAt;
    private Instant updatedAt;
}
```

### 5.2 Repository 接口

```java
public interface ToolConfigRepository extends MongoRepository<ToolConfigDocument, String> {
    
    /**
     * 查询用户的所有工具配置
     */
    List<ToolConfigDocument> findByUserId(String userId);
    
    /**
     * 查询用户的单个工具配置
     */
    Optional<ToolConfigDocument> findByUserIdAndToolName(String userId, String toolName);
    
    /**
     * 批量查询用户的多个工具配置
     */
    List<ToolConfigDocument> findByUserIdAndToolNameIn(String userId, List<String> toolNames);
    
    /**
     * 查询用户未自定义的工具（用于同步）
     */
    List<ToolConfigDocument> findByUserIdAndCustomizedFalse(String userId);
}
```

### 5.3 系统工具注册表

```java
@Component
public class SystemToolRegistry {
    
    private final Map<String, SystemToolDefinition> tools = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        // 注册所有系统工具
        register(SystemToolDefinition.builder()
            .toolName("run_shell_command")
            .description("执行 Shell 命令，用于运行脚本、安装依赖等")
            .category(ToolCategory.SANDBOX)
            .runEnvironment(RunEnvironment.SANDBOX)
            .riskLevel(ToolRiskLevel.HIGH)
            .enabled(true)
            .approvalRequired(true)
            .denyPatterns(List.of("rm -rf", "del /f", "format ", "shutdown ", "reboot "))
            .examples(List.of(
                new ToolExample("安装 Python 包", "{\"command\": \"pip install requests\"}")
            ))
            .build());
        // ... 注册其他工具
    }
    
    public SystemToolDefinition getTool(String toolName) { ... }
    public Collection<SystemToolDefinition> getAllTools() { ... }
}
```

### 5.4 Service 层

```java
@Service
public class ToolConfigServiceImpl implements IToolConfigService {
    
    private final ToolConfigRepository repository;
    private final SystemToolRegistry systemRegistry;
    
    @Override
    public List<ToolConfigDTO> listByUserId(String userId) {
        // 查询用户配置
        List<ToolConfigDocument> userConfigs = repository.findByUserId(userId);
        
        // 如果是新用户，同步系统工具
        if (userConfigs.isEmpty()) {
            syncSystemTools(userId);
            userConfigs = repository.findByUserId(userId);
        }
        
        return toDTOs(userConfigs);
    }
    
    @Override
    public ToolConfigDTO updateToolConfig(String userId, String toolName, ToolConfigUpdateCommand command) {
        // 查询现有配置
        ToolConfigDocument config = repository.findByUserIdAndToolName(userId, toolName)
            .orElseGet(() -> createFromSystemDefault(userId, toolName));
        
        // 更新字段
        if (command.getRiskLevel() != null) {
            config.setRiskLevel(command.getRiskLevel());
        }
        if (command.getEnabled() != null) {
            config.setEnabled(command.getEnabled());
        }
        if (command.getApprovalRequired() != null) {
            config.setApprovalRequired(command.getApprovalRequired());
        }
        config.setCustomized(true);
        config.setUpdatedAt(Instant.now());
        
        return toDTO(repository.save(config));
    }
    
    @Override
    @Transactional
    public int syncSystemTools(String userId) {
        // 获取系统工具
        Collection<SystemToolDefinition> systemTools = systemRegistry.getAllTools();
        
        // 查询用户已有工具名称
        List<ToolConfigDocument> existingConfigs = repository.findByUserId(userId);
        Set<String> existingToolNames = existingConfigs.stream()
            .map(ToolConfigDocument::getToolName)
            .collect(Collectors.toSet());
        
        // 找出新增工具
        List<ToolConfigDocument> newConfigs = systemTools.stream()
            .filter(tool -> !existingToolNames.contains(tool.getToolName()))
            .map(tool -> createFromSystemDefinition(userId, tool))
            .collect(Collectors.toList());
        
        // 批量保存
        if (!newConfigs.isEmpty()) {
            repository.saveAll(newConfigs);
        }
        
        return newConfigs.size();
    }
}
```

### 5.5 ToolGuardEngine 集成改造

```java
@Component
public class ToolGuardEngine {
    
    private final ToolConfigProvider configProvider;  // 新增依赖
    private final TdAgentProperties properties;
    private final ObjectMapper objectMapper;
    
    public ToolGuardDecision evaluate(String toolName, Map<String, Object> input, String userId) {
        // 1. 获取用户工具配置
        ToolConfigDTO config = configProvider.getEffectiveConfig(toolName, userId);
        
        // 2. 检查启用状态
        if (!config.getEnabled()) {
            return deny(toolName, "工具已禁用：" + toolName);
        }
        
        // 3. 危险命令检测（使用用户配置的 denyPatterns）
        String normalized = stringify(input).toLowerCase(Locale.ROOT);
        for (String pattern : config.getDenyPatterns()) {
            if (normalized.contains(pattern)) {
                return ToolGuardDecision.builder()
                    .toolName(toolName)
                    .allowed(false)
                    .riskLevel(ToolRiskLevel.CRITICAL)
                    .reason("命中拒绝模式：" + pattern)
                    .matchedPattern(pattern)
                    .build();
            }
        }
        
        // 4. 高风险工具审批（使用用户配置的 approvalRequired 和 riskLevel）
        if (config.getApprovalRequired()) {
            return ToolGuardDecision.builder()
                .toolName(toolName)
                .allowed(true)
                .requiresApproval(true)
                .riskLevel(config.getRiskLevel())
                .reason("高风险工具，需要审批")
                .matchedPattern(toolName)
                .build();
        }
        
        // 5. 默认放行
        return allow(toolName, config.getRiskLevel(), "工具调用通过安全检查");
    }
}
```

### 5.6 TdAgentToolkitFactory 集成改造

```java
@Component
public class TdAgentToolkitFactory {
    
    private final ToolConfigProvider configProvider;  // 新增依赖
    
    private void registerTool(Toolkit toolkit, ConversationSessionContext context, AgentTool tool) {
        String toolName = tool.getName();
        String userId = context.getUserId();
        
        // 获取用户工具配置
        ToolConfigDTO config = configProvider.getEffectiveConfig(toolName, userId);
        
        // 如果禁用，跳过注册
        if (!config.getEnabled()) {
            log.debug("[Toolkit] 跳过已禁用工具: {}, userId: {}", toolName, userId);
            return;
        }
        
        // 如果启用 Tool Guard，包装
        if (properties.getToolGuard().isEnabled()) {
            toolkit.registerTool(new GuardedAgentTool(tool, context, toolGuardEngine, toolApprovalService));
            return;
        }
        
        toolkit.registerTool(tool);
    }
}
```

### 5.7 API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/agent/tool-config` | 获取当前用户所有工具配置 |
| `GET` | `/api/agent/tool-config/{toolName}` | 获取单个工具配置详情 |
| `PUT` | `/api/agent/tool-config/{toolName}` | 更新工具配置（风险等级/启用状态） |
| `POST` | `/api/agent/tool-config/sync` | 同步系统新增工具到用户配置 |
| `GET` | `/api/agent/tool-config/system-defaults` | 查看系统默认工具清单（不创建用户配置） |

### 5.8 请求/响应示例

#### GET /api/agent/tool-config

**Response:**
```json
[
  {
    "toolName": "run_shell_command",
    "description": "执行 Shell 命令，用于运行脚本、安装依赖等",
    "category": "sandbox",
    "runEnvironment": "sandbox",
    "riskLevel": "HIGH",
    "enabled": true,
    "approvalRequired": true,
    "denyPatterns": ["rm -rf", "del /f", "format ", "shutdown ", "reboot "],
    "examples": [
      {
        "title": "安装 Python 包",
        "input": "{\"command\": \"pip install requests\"}"
      }
    ],
    "customized": false,
    "updatedAt": "2026-05-05T10:00:00Z"
  }
]
```

#### PUT /api/agent/tool-config/run_shell_command

**Request:**
```json
{
  "riskLevel": "CRITICAL",
  "enabled": false
}
```

**Response:**
```json
{
  "toolName": "run_shell_command",
  "riskLevel": "CRITICAL",
  "enabled": false,
  "customized": true,
  "updatedAt": "2026-05-05T12:00:00Z"
}
```

#### POST /api/agent/tool-config/sync

**Response:**
```json
{
  "synced": 3,
  "message": "已同步 3 个新工具"
}
```

---

## 6. 前端设计

### 6.1 页面布局

```
┌─────────────────────────────────────────────────┐
│ 工具库                                    [同步] │
│ 管理智能体可用工具、风险等级和启用状态           │
├─────────────────────────────────────────────────┤
│ [全部] [沙盒] [浏览器] [内置]  🔍 搜索工具...   │
├─────────────────────────────────────────────────┤
│ ┌──────────────┐ ┌──────────────┐              │
│ │ run_shell_   │ │ fs_read_file │              │
│ │ command      │ │              │              │
│ │ [HIGH] [ON]  │ │ [LOW]  [ON]  │              │
│ │              │ │              │              │
│ │ 执行Shell命令│ │ 读取文件内容 │              │
│ │ 示例: pip... │ │ 示例: cat... │              │
│ │              │ │              │ │
│ │ [风险▼] [OFF]│ │ [风险▼] [ON] │              │
│ └──────────────┘ └──────────────┘              │
└─────────────────────────────────────────────────┘
```

### 6.2 组件结构

```
ToolLibrary.tsx
├── ToolFilterBar（筛选栏）
│   ├── 分类 Tab 切换
│   └── 搜索框
├── ToolCardGrid（工具卡片网格）
│   └── ToolCard（单个工具卡片）
│       ├── 工具名称 + 分类标签
│       ├── 风险等级 Tag（可点击修改）
│       ├── 描述文本
│       ├── 使用示例（折叠面板）
│       ├── 运行环境 Tag
│       └── 开关 + 风险等级下拉框
└── SyncButton（同步按钮）
```

### 6.3 状态管理

```typescript
interface ToolConfig {
  toolName: string;
  description: string;
  category: 'builtin' | 'sandbox' | 'browser' | 'custom';
  runEnvironment: 'system' | 'sandbox';
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  enabled: boolean;
  approvalRequired: boolean;
  denyPatterns: string[];
  examples: Array<{ title: string; input: string }>;
  customized: boolean;
  updatedAt: string;
}

// 页面状态
const [tools, setTools] = useState<ToolConfig[]>([]);
const [loading, setLoading] = useState(false);
const [filter, setFilter] = useState<'all' | 'sandbox' | 'browser' | 'builtin'>('all');
const [search, setSearch] = useState('');
```

### 6.4 API 调用

```typescript
// api.ts 新增方法
export interface ToolConfigAPI {
  listToolConfigs(): Promise<ToolConfig[]>;
  updateToolConfig(toolName: string, data: {
    riskLevel?: string;
    enabled?: boolean;
    approvalRequired?: boolean;
  }): Promise<ToolConfig>;
  syncSystemTools(): Promise<{ synced: number }>;
}

// ToolLibrary.tsx 中使用
useEffect(() => {
  api.listToolConfigs().then(setTools);
}, []);

const handleRiskChange = (toolName: string, newLevel: string) => {
  api.updateToolConfig(toolName, { riskLevel: newLevel }).then(updated => {
    setTools(tools.map(t => t.toolName === toolName ? updated : t));
  });
};
```

---

## 7. 安全与权限

### 7.1 用户隔离

- 所有工具配置按 `userId` 隔离
- `SecurityUtils.getCurrentUserId()` 获取当前用户
- 禁止跨用户修改配置

### 7.2 权限控制

- **查看**：所有登录用户可查看自己的工具配置
- **修改**：普通用户可修改自己的配置（风险等级、启用状态）
- **系统默认**：查看系统默认配置无需权限

### 7.3 审计日志

工具配置变更记录日志：
```
[ToolConfig] 用户 user-123 修改工具 run_shell_command: 
  riskLevel: HIGH -> CRITICAL, enabled: true -> false
```

---

## 8. 实现计划

### 8.1 阶段一：数据模型与存储（MongoDB）

✅ 创建 `ToolConfigDocument` 文档类  
✅ 创建 `ToolConfigRepository` 接口  
✅ 创建系统工具注册表 `SystemToolRegistry`  
✅ 创建工具分类/环境枚举  

### 8.2 阶段二：Service 层与 API

✅ 创建 `IToolConfigService` 接口和实现  
✅ 创建 `ToolConfigController` 提供 REST API  
✅ 创建 DTO 和 Command 对象  

### 8.3 阶段三：集成现有系统

✅ 改造 `ToolGuardEngine` 读取用户配置  
✅ 改造 `TdAgentToolkitFactory` 应用启用状态  
✅ 创建 `ToolConfigProvider` 工具配置提供者  

### 8.4 阶段四：前端实现

✅ 更新 `api.ts` 添加 API 方法  
✅ 实现 `ToolLibrary.tsx` 页面  
✅ 添加风险等级修改组件  
✅ 添加工具启用/禁用开关  
✅ 添加同步按钮  

### 8.5 阶段五：测试与验证

✅ 测试工具配置查询  
✅ 测试风险等级修改  
✅ 测试同步功能  
✅ 测试 ToolGuardEngine 集成  
✅ 测试 TdAgentToolkitFactory 集成  

---

## 9. 风险与注意事项

### 9.1 性能影响

- **问题**：每次工具调用都查询 MongoDB 可能影响性能
- **方案**：使用 Caffeine 本地缓存，TTL 5 分钟

### 9.2 并发更新

- **问题**：多端同时修改工具配置可能冲突
- **方案**：MongoDB 乐观锁（`@Version` 字段）

### 9.3 新用户初始化

- **问题**：新用户首次访问需同步系统工具
- **方案**：`listByUserId()` 自动检测并同步

### 9.4 系统工具升级

- **问题**：系统新增工具时用户如何感知
- **方案**：提供"同步"按钮，用户手动触发

---

## 10. 后续扩展

### 10.1 工具使用统计

结合 Token Usage 系统，统计每个工具被调用次数、成功率、平均耗时。

### 10.2 工具权限角色

引入角色体系（管理员/普通用户），管理员可配置全局工具策略。

### 10.3 工具自定义

允许用户上传自定义脚本，注册为新工具。

---

**文档版本**: v1.0  
**创建时间**: 2026-05-05  
**作者**: LiangshouX
