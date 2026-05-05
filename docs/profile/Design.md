# Profile System Prompt 热更新详细设计

## 1. 设计概述

### 1.1 目标
实现按用户个性化配置的 System Prompt 热更新机制，允许用户在线编辑和更新 Agent 的"人设"、"能力边界"等设定，无需重启应用即可生效。

### 1.2 核心原则
- **用户隔离**：每个用户拥有独立的 profile 配置
- **优先级**：MongoDB 用户自定义 > resources 默认文件
- **热更新**：更新后立即生效，无需重启
- **降级兜底**：如果 MongoDB 中没有配置，使用 resources 下的默认文件
- **可选性**：所有 profile 文件都是可选的，不存在不报错

### 1.3 技术栈
- **后端**：Spring Boot + MongoDB + AgentScope-Java
- **前端**：React + TypeScript + Ant Design
- **通信**：RESTful API

---

## 2. 系统架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                         前端 (React)                         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  CourtRules.tsx (工作台页面)                          │   │
│  │  - 文件列表展示 (SOUL.md, AGENTS.md, PROFILE.md)     │   │
│  │  - Markdown 编辑/预览                                │   │
│  │  - 保存/重置/上传/下载操作                           │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────┬───────────────────────────────────┘
                          │ RESTful API
                          │ HTTP/JSON
┌─────────────────────────┴───────────────────────────────────┐
│                    后端 (Spring Boot)                        │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Controller 层                                        │   │
│  │  - TdAgentProfileController                          │   │
│  └──────────────────────────────────────────────────────┘   │
│                          │                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Service 层                                           │   │
│  │  - TdAgentProfileService (新增)                      │   │
│  │  - TdAgentPromptService (改造)                       │   │
│  └──────────────────────────────────────────────────────┘   │
│                          │                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Repository 层                                        │   │
│  │  - AgentProfileRepository (新增)                     │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────┴───────────────────────────────────┐
│                      MongoDB                                 │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  agent_profiles (新增 collection)                    │   │
│  │  - userId + filename 复合唯一索引                     │   │
│  │  - 存储用户自定义的 profile 文件内容                  │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  Resources (默认文件)                        │
│  profiles/                                                  │
│  ├── AGENTS.md (默认工作流规则)                             │
│  ├── SOUL.md (默认核心身份)                                 │
│  └── PROFILE.md (默认用户画像)                              │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 数据流

#### 2.2.1 用户注册时
```
用户注册 (tang-dynasty-backend)
    │
    ├─ 写入 MySQL sys_user 表
    │
    └─ 触发 Profile 初始化事件 (异步)
           │
           └─ TdAgentProfileService.initializeUser(userId)
                  │
                  ├─ 读取 resources/profiles/ 默认文件
                  ├─ 为每个文件创建 AgentProfileDocument
                  └─ 批量保存到 MongoDB agent_profiles collection
```

#### 2.2.2 Agent 创建时 (对话流程)
```
用户发起对话
    │
    └─ TdAgentFactory.createAgent(context)
           │
           └─ TdAgentPromptService.buildPrompt(context)
                  │
                  ├─ TdAgentProfileService.loadUserProfiles(userId)
                  │     │
                  │     ├─ 查询 MongoDB agent_profiles (按 userId)
                  │     ├─ 如果有自定义配置 → 返回用户配置
                  │     └─ 如果没有 → 读取 resources/profiles/ 默认文件
                  │
                  ├─ ProfilePromptBuilder.build(profiles)
                  │     │
                  │     ├─ 按顺序加载: SOUL.md → AGENTS.md → PROFILE.md
                  │     ├─ 剥离 YAML frontmatter
                  │     ├─ 处理 heartbeat 标记
                  │     └─ 拼接为完整 prompt
                  │
                  └─ 返回完整 System Prompt
                         │
                         └─ 传入 ReActAgent.sysPrompt()
```

#### 2.2.3 用户更新 Profile 时
```
前端 CourtRules.tsx 编辑文件
    │
    └─ PUT /api/v1/tdagent/profiles/{filename}
           │
           └─ TdAgentProfileController.updateProfile()
                  │
                  ├─ 校验文件内容
                  ├─ TdAgentProfileService.updateProfile(userId, filename, content)
                  │     │
                  │     ├─ 查询或创建 AgentProfileDocument
                  │     ├─ 更新 content 字段
                  │     └─ 保存到 MongoDB
                  │
                  └─ 返回更新结果
                         │
                         └─ 下次 Agent 创建时自动生效 (热更新)
```

---

## 3. 数据模型设计

### 3.1 MongoDB Document: AgentProfileDocument

```java
@Document(collection = "agent_profiles")
@CompoundIndex(
    name = "uk_user_filename", 
    def = "{'userId': 1, 'filename': 1}", 
    unique = true
)
public class AgentProfileDocument {
    
    @Id
    private String id;                          // 主键，格式: "{userId}:{filename}"
    
    @Field("user_id")
    private String userId;                      // 用户ID
    
    @Field("file_name")
    private String filename;                    // 文件名: SOUL.md, AGENTS.md, PROFILE.md
    
    @Field("content")
    private String content;                     // 文件内容 (Markdown 格式)
    
    @Field("enabled")
    private boolean enabled;                    // 是否启用 (对应前端的 active 开关)
    
    @Field("source")
    private ProfileSource source;               // 来源: DEFAULT (默认), USER_CUSTOMIZED (用户自定义)
    
    @Field("version")
    @Version
    private Long version;                       // 乐观锁
    
    @Field("created_at")
    private Instant createdAt;                  // 创建时间
    
    @Field("updated_at")
    private Instant updatedAt;                  // 更新时间
}

public enum ProfileSource {
    DEFAULT,           // 从 resources 加载的默认配置
    USER_CUSTOMIZED    // 用户自定义配置
}
```

**设计说明：**
- 使用 `userId + filename` 复合唯一索引，确保每个用户的每个文件只有一条记录
- `enabled` 字段支持前端的启用/禁用开关
- `source` 字段区分是默认配置还是用户自定义，便于管理和统计
- `version` 乐观锁防止并发更新冲突

### 3.2 默认 Profile 文件

位置：`tang-dynasty-agent-engine/src/main/resources/profiles/`

| 文件名 | 用途 | 是否必填 |
|--------|------|----------|
| AGENTS.md | 工作流规则、业务指引、能力边界定义 | 否 (可选) |
| SOUL.md | 核心身份、行为原则、价值观、语气风格 | 否 (可选) |
| PROFILE.md | 用户画像、偏好、历史交互学习到的信息 | 否 (可选) |

**加载顺序：** SOUL.md → AGENTS.md → PROFILE.md

---

## 4. API 接口设计

### 4.1 Controller: TdAgentProfileController

**基础路径：** `/api/v1/tdagent/profiles`

所有接口都需要用户认证，通过 JWT token 获取用户身份。

#### 4.1.1 获取用户的 Profile 列表

```
GET /api/v1/tdagent/profiles
```

**请求头：**
```
Authorization: Bearer {jwt_token}
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "filename": "SOUL.md",
      "content": "# 三省六部 AI 助手灵魂设定\n...",
      "enabled": true,
      "source": "USER_CUSTOMIZED",
      "size": "1.6 KB",
      "updatedAt": "2026-05-05T10:30:00Z"
    },
    {
      "filename": "AGENTS.md",
      "content": "# 智能体定义\n...",
      "enabled": true,
      "source": "DEFAULT",
      "size": "5.6 KB",
      "updatedAt": "2026-05-01T08:00:00Z"
    }
  ]
}
```

#### 4.1.2 获取单个 Profile 文件

```
GET /api/v1/tdagent/profiles/{filename}
```

**路径参数：**
- `filename`: 文件名 (SOUL.md, AGENTS.md, PROFILE.md)

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "filename": "SOUL.md",
    "content": "# 三省六部 AI 助手灵魂设定\n...",
    "enabled": true,
    "source": "USER_CUSTOMIZED",
    "updatedAt": "2026-05-05T10:30:00Z"
  }
}
```

#### 4.1.3 更新 Profile 文件

```
PUT /api/v1/tdagent/profiles/{filename}
```

**路径参数：**
- `filename`: 文件名

**请求体：**
```json
{
  "content": "# 新的灵魂设定\n...",
  "enabled": true
}
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "filename": "SOUL.md",
    "enabled": true,
    "updatedAt": "2026-05-05T10:30:00Z"
  }
}
```

#### 4.1.4 批量更新 Profile 文件

```
PUT /api/v1/tdagent/profiles/batch
```

**请求体：**
```json
{
  "profiles": [
    {
      "filename": "SOUL.md",
      "content": "# 新灵魂设定\n...",
      "enabled": true
    },
    {
      "filename": "AGENTS.md",
      "content": "# 新规则\n...",
      "enabled": false
    }
  ]
}
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "updatedCount": 2,
    "updatedAt": "2026-05-05T10:30:00Z"
  }
}
```

#### 4.1.5 重置 Profile 文件为默认值

```
POST /api/v1/tdagent/profiles/{filename}/reset
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "filename": "SOUL.md",
    "source": "DEFAULT",
    "updatedAt": "2026-05-05T10:30:00Z"
  }
}
```

#### 4.1.6 下载 Profile 文件

```
GET /api/v1/tdagent/profiles/{filename}/download
```

**响应：**
```
Content-Type: text/markdown
Content-Disposition: attachment; filename="SOUL.md"

# 灵魂设定内容...
```

#### 4.1.7 上传 Profile 文件

```
POST /api/v1/tdagent/profiles/{filename}/upload
```

**请求体：** (multipart/form-data)
```
file: (二进制文件内容)
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "filename": "SOUL.md",
    "enabled": true,
    "updatedAt": "2026-05-05T10:30:00Z"
  }
}
```

---

## 5. 核心类设计

### 5.1 Service: TdAgentProfileService

```java
@Service
public class TdAgentProfileService {
    
    private static final String PROFILES_PATH = "profiles/";
    private static final List<String> DEFAULT_FILES = 
            List.of("SOUL.md", "AGENTS.md", "PROFILE.md");
    
    private final AgentProfileRepository profileRepository;
    
    /**
     * 为新用户初始化默认 Profile
     * 在用户注册时调用，从 resources/profiles/ 读取默认文件并保存到 MongoDB
     */
    @Transactional
    public void initializeUser(String userId);
    
    /**
     * 加载用户的所有 Profile 文件
     * 优先从 MongoDB 读取用户自定义配置，如果没有则使用 resources 默认文件
     */
    public List<AgentProfileDocument> loadUserProfiles(String userId);
    
    /**
     * 更新单个 Profile 文件
     */
    @Transactional
    public AgentProfileDocument updateProfile(String userId, String filename, 
                                               String content, boolean enabled);
    
    /**
     * 批量更新 Profile 文件
     */
    @Transactional
    public int batchUpdateProfiles(String userId, List<ProfileUpdateRequest> requests);
    
    /**
     * 重置 Profile 文件为默认值
     */
    @Transactional
    public AgentProfileDocument resetProfile(String userId, String filename);
    
    /**
     * 获取单个 Profile 文件 (从 MongoDB 或 resources)
     */
    public Optional<AgentProfileDocument> getProfile(String userId, String filename);
    
    /**
     * 获取用户的所有 Profile 列表
     */
    public List<AgentProfileDocument> listProfiles(String userId);
    
    /**
     * 从 resources 读取默认文件内容
     */
    private String loadDefaultFile(String filename);
    
    /**
     * 剥离 YAML frontmatter (--- 开头的元数据块)
     */
    private String stripFrontmatter(String content);
}
```

### 5.2 Repository: AgentProfileRepository

```java
public interface AgentProfileRepository extends MongoRepository<AgentProfileDocument, String> {
    
    /**
     * 根据用户ID查询所有 Profile
     */
    List<AgentProfileDocument> findByUserId(String userId);
    
    /**
     * 根据用户ID和文件名查询
     */
    Optional<AgentProfileDocument> findByUserIdAndFilename(String userId, String filename);
    
    /**
     * 检查用户是否已有指定的 Profile
     */
    boolean existsByUserIdAndFilename(String userId, String filename);
    
    /**
     * 删除用户的所有 Profile
     */
    void deleteByUserId(String userId);
}
```

### 5.3 改造 TdAgentPromptService

原 `TdAgentPromptService.buildPrompt()` 方法需要改造，集成 Profile 加载：

```java
@Service
public class TdAgentPromptService {

    private final TdAgentProperties properties;
    private final IConversationPersistenceService persistenceService;
    private final TdAgentProfileService profileService;  // 新增依赖
    
    /**
     * 构建完整的 System Prompt
     * 
     * 新增逻辑：
     * 1. 加载用户 Profile (SOUL.md, AGENTS.md, PROFILE.md)
     * 2. 使用 ProfilePromptBuilder 构建 Profile 部分
     * 3. 将 Profile prompt 注入到原有的 system prompt 模板中
     */
    public String buildPrompt(ConversationSessionContext context) {
        // 1. 加载用户 Profile
        List<AgentProfileDocument> profiles = profileService.loadUserProfiles(context.getUserId());
        
        // 2. 构建 Profile 部分
        String profilePrompt = ProfilePromptBuilder.build(profiles);
        
        // 3. 构建原有的 system prompt
        String basePrompt = buildBasePrompt(context);
        
        // 4. 组合：Profile Prompt + Base Prompt
        if (profilePrompt != null && !profilePrompt.isBlank()) {
            return profilePrompt + "\n\n" + basePrompt;
        }
        return basePrompt;
    }
    
    private String buildBasePrompt(ConversationSessionContext context) {
        // 原有的 buildPrompt 逻辑保持不变
        // ...
    }
}
```

### 5.4 新增 ProfilePromptBuilder

参考 Python 实现，新增 Profile 专用的 prompt 构建器：

```java
public class ProfilePromptBuilder {
    
    private static final Pattern HEARTBEAT_PATTERN = Pattern.compile(
            r"<!-- heartbeat:start -->.*?<!-- heartbeat:end -->",
            Pattern.DOTALL
    );
    
    /**
     * 构建 Profile 部分的 System Prompt
     * 
     * @param profiles 用户的所有 Profile 文件
     * @return 拼接后的 prompt 字符串
     */
    public static String build(List<AgentProfileDocument> profiles) {
        List<String> parts = new ArrayList<>();
        
        // 按顺序加载: SOUL.md → AGENTS.md → PROFILE.md
        List<String> loadOrder = List.of("SOUL.md", "AGENTS.md", "PROFILE.md");
        
        for (String filename : loadOrder) {
            Optional<AgentProfileDocument> opt = profiles.stream()
                    .filter(p -> p.getFilename().equals(filename))
                    .findFirst();
            
            if (opt.isPresent()) {
                AgentProfileDocument profile = opt.get();
                
                // 如果禁用，跳过
                if (!profile.isEnabled()) {
                    continue;
                }
                
                String content = profile.getContent();
                
                // 剥离 YAML frontmatter
                content = stripFrontmatter(content);
                
                // AGENTS.md 特殊处理: heartbeat
                if ("AGENTS.md".equals(filename)) {
                    content = processHeartbeat(content);
                }
                
                if (content != null && !content.isBlank()) {
                    parts.add("# " + filename);
                    parts.add(content);
                }
            }
        }
        
        // 降级兜底
        if (parts.isEmpty()) {
            return "";
        }
        
        return String.join("\n\n", parts);
    }
    
    /**
     * 剥离 YAML frontmatter (--- 开头的元数据块)
     */
    private static String stripFrontmatter(String content) {
        if (content.startsWith("---")) {
            String[] parts = content.split("---", 3);
            if (parts.length >= 3) {
                return parts[2].trim();
            }
        }
        return content.trim();
    }
    
    /**
     * 处理 heartbeat 标记
     * 根据配置决定是否保留 heartbeat 段
     */
    private static String processHeartbeat(String content) {
        // TODO: 根据 heartbeat_enabled 配置处理
        // 当前版本: 移除 heartbeat 标记
        content = HEARTBEAT_PATTERN.matcher(content).replaceAll("");
        content = content.replace("<!-- heartbeat:start -->", "");
        content = content.replace("<!-- heartbeat:end -->", "");
        return content;
    }
}
```

### 5.5 DTO 设计

#### ProfileListResponse.java
```java
@Data
@Builder
public class ProfileListResponse {
    private String filename;
    private String content;
    private boolean enabled;
    private String source;      // "DEFAULT" or "USER_CUSTOMIZED"
    private String size;        // "1.6 KB"
    private Instant updatedAt;
}
```

#### ProfileUpdateRequest.java
```java
@Data
public class ProfileUpdateRequest {
    @NotBlank
    private String filename;
    
    @NotBlank
    private String content;
    
    private boolean enabled;
}
```

#### BatchUpdateRequest.java
```java
@Data
public class BatchUpdateRequest {
    @Valid
    @NotEmpty
    private List<ProfileUpdateRequest> profiles;
}
```

---

## 6. 用户注册集成设计

### 6.1 问题分析

当前用户注册在 `tang-dynasty-backend` 模块中，而 Profile 管理在 `tang-dynasty-agent-engine` 模块。有两种集成方案：

**方案 A：跨模块 RPC 调用**
- `tang-dynasty-backend` 注册完成后，调用 `tang-dynasty-agent-engine` 的 API
- 优点：解耦，职责清晰
- 缺点：需要引入跨模块通信（HTTP/RPC），增加复杂度

**方案 B：异步事件/监听器**
- `tang-dynasty-backend` 注册完成后，发布 Spring Event
- `tang-dynasty-agent-engine` 监听事件并初始化 Profile
- 优点：解耦，异步不阻塞注册流程
- 缺点：需要引入事件机制

**方案 C：在 Agent Engine 中提供初始化接口，由 Backend 调用**
- `tang-dynasty-agent-engine` 提供 `POST /api/v1/tdagent/profiles/init/{userId}` 接口
- `tang-dynasty-backend` 在注册完成后调用该接口
- 优点：简单直接，无需引入额外机制
- 缺点：需要跨模块 HTTP 调用

**✅ 推荐方案：方案 D - 在服务层直接依赖**

由于两个模块都在同一个 Spring Boot 应用中运行（从启动类判断），可以直接在 `tang-dynasty-backend` 的 `SysUserServiceImpl` 中注入 `TdAgentProfileService` 并调用。

### 6.2 实现方案

修改 `SysUserServiceImpl.register()` 方法：

```java
@Service
public class SysUserServiceImpl implements SysUserService {
    
    private final TdAgentProfileService profileService;  // 新增依赖
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUserVO register(SysUserDTO dto) {
        // 原有的注册逻辑
        String userId = normalizeRequired(dto.getUserId(), "userId 不能为空");
        if (support.lambdaQuery().eq(SysUserPO::getUserId, userId).exists()) {
            throw new IllegalArgumentException("userId 已存在");
        }

        SysUserPO po = new SysUserPO();
        po.setUserId(userId);
        po.setPassword(passwordEncoder.encode(normalizeRequired(dto.getPassword(), "password 不能为空")));
        po.setNickname(normalizeNickname(dto.getNickname(), userId));
        po.setRole(normalizeRole(dto.getRole()));
        support.save(po);
        
        // 新增：初始化 Profile
        try {
            profileService.initializeUser(userId);
        } catch (Exception e) {
            log.error("初始化用户 Profile 失败 - userId: {}, error: {}", userId, e.getMessage(), e);
            // 不阻塞注册流程，Profile 初始化失败不影响注册成功
            // 可以在后续通过定时任务补偿
        }
        
        return toVO(po);
    }
}
```

**注意事项：**
- Profile 初始化失败不应阻塞用户注册流程
- 可以通过定时任务或手动触发补偿初始化
- 需要在 `tang-dynasty-backend` 的 pom.xml 中添加对 `tang-dynasty-agent-engine` 的依赖

---

## 7. 前端联调设计

### 7.1 API 调用封装

在 `website/src/services/profileApi.ts` 中新增：

```typescript
import { api } from '../api';

export const profileApi = {
  /**
   * 获取用户的 Profile 列表
   */
  listProfiles: () => 
    api.get('/api/v1/tdagent/profiles').catch(() => ({ data: [] })),
  
  /**
   * 获取单个 Profile
   */
  getProfile: (filename: string) => 
    api.get(`/api/v1/tdagent/profiles/${filename}`).catch(() => null),
  
  /**
   * 更新 Profile
   */
  updateProfile: (filename: string, data: { content: string; enabled?: boolean }) => 
    api.put(`/api/v1/tdagent/profiles/${filename}`, data),
  
  /**
   * 批量更新 Profile
   */
  batchUpdateProfiles: (data: { profiles: Array<{ filename: string; content: string; enabled: boolean }> }) => 
    api.put('/api/v1/tdagent/profiles/batch', data),
  
  /**
   * 重置 Profile 为默认值
   */
  resetProfile: (filename: string) => 
    api.post(`/api/v1/tdagent/profiles/${filename}/reset`),
  
  /**
   * 下载 Profile 文件
   */
  downloadProfile: (filename: string) => 
    api.get(`/api/v1/tdagent/profiles/${filename}/download`, { responseType: 'blob' }),
  
  /**
   * 上传 Profile 文件
   */
  uploadProfile: (filename: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post(`/api/v1/tdagent/profiles/${filename}/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};
```

### 7.2 CourtRules.tsx 改造

改造要点：

1. **移除硬编码的 initialFiles**，改为从 API 加载
2. **保存操作**调用 `profileApi.updateProfile()`
3. **启用/禁用开关**调用 `profileApi.updateProfile()` 更新 enabled 字段
4. **重置按钮**调用 `profileApi.resetProfile()`
5. **上传/下载**调用对应 API
6. **增加 loading 状态**和错误处理

```typescript
const CourtRules: React.FC = () => {
  const [activeFile, setActiveFile] = useState('SOUL.md');
  const [files, setFiles] = useState<Record<string, ProfileFile>>({});
  const [preview, setPreview] = useState(true);
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);

  // 加载 Profile 列表
  useEffect(() => {
    loadProfiles();
  }, []);

  const loadProfiles = async () => {
    setFetching(true);
    try {
      const result = await profileApi.listProfiles();
      const filesMap: Record<string, ProfileFile> = {};
      result.data.forEach((profile: any) => {
        filesMap[profile.filename] = {
          content: profile.content,
          enabled: profile.enabled,
          size: profile.size,
          time: formatTime(profile.updatedAt),
          source: profile.source,
        };
      });
      setFiles(filesMap);
    } catch (error) {
      message.error('加载 Profile 失败');
    } finally {
      setFetching(false);
    }
  };

  const handleSave = async () => {
    setLoading(true);
    try {
      await profileApi.updateProfile(activeFile, {
        content: files[activeFile].content,
        enabled: files[activeFile].enabled,
      });
      message.success(`${activeFile} 已保存`);
      await loadProfiles(); // 重新加载
    } catch (error) {
      message.error('保存失败');
    } finally {
      setLoading(false);
    }
  };

  // ... 其他逻辑类似
};
```

---

## 8. 项目结构设计

### 8.1 后端目录结构

```
tang-dynasty-agent-engine/
├── src/main/java/com/liangshou/tangdynasty/agentic/
│   ├── adapter/controller/
│   │   └── TdAgentProfileController.java          [新增] Profile 管理 Controller
│   ├── application/
│   │   ├── ITdAgentProfileService.java             [新增] Profile 服务接口
│   │   └── dto/
│   │       ├── ProfileListResponse.java            [新增] Profile 列表响应
│   │       ├── ProfileUpdateRequest.java           [新增] Profile 更新请求
│   │       └── BatchUpdateRequest.java             [新增] 批量更新请求
│   ├── domain/
│   │   └── profile/
│   │       ├── model/
│   │       │   └── AgentProfileDocument.java       [新增] Profile 文档实体
│   │       └── enums/
│   │           └── ProfileSource.java              [新增] Profile 来源枚举
│   ├── infrastructure/mongo/repository/
│   │   └── AgentProfileRepository.java             [新增] Profile Repository
│   └── common/util/
│       ├── SoulPromptLoader.java                   [现有] 需调整为从 MongoDB 加载
│       └── ProfilePromptBuilder.java               [新增] Profile Prompt 构建器
│
└── src/main/resources/
    └── profiles/                                   [现有] 默认文件目录
        ├── AGENTS.md
        ├── SOUL.md
        └── PROFILE.md
```

### 8.2 前端目录结构

```
website/
├── src/
│   ├── services/
│   │   └── profileApi.ts                           [新增] Profile API 调用
│   ├── pages/Censorate/
│   │   └── CourtRules.tsx                          [改造] 对接真实 API
│   └── types/
│       └── profile.ts                              [新增] Profile 类型定义
```

---

## 9. 关键流程时序图

### 9.1 用户注册流程

```
用户         Backend         Agent Engine        MongoDB
 │             │                 │                  │
 │─注册请求──>│                 │                  │
 │             │                 │                  │
 │             │─创建用户记录──>│                  │
 │             │  (MySQL)       │                  │
 │             │                 │                  │
 │             │─initializeUser─>│                  │
 │             │  (userId)      │                  │
 │             │                 │─读取默认文件──>│
 │             │                 │  (resources)    │
 │             │                 │                  │
 │             │                 │─创建 Profile──>│
 │             │                 │  Documents      │
 │             │                 │                  │
 │             │<────────────────┼─保存──────────│
 │             │                 │                  │
 │<─注册成功──│                 │                  │
 │             │                 │                  │
```

### 9.2 对话时构建 System Prompt 流程

```
用户         Frontend        Backend             MongoDB
 │             │                 │                  │
 │─发送消息──>│                 │                  │
 │             │                 │                  │
 │             │─POST /chat────>│                  │
 │             │                 │                  │
 │             │                 │─createAgent────>│
 │             │                 │                  │
 │             │                 │─buildPrompt────>│
 │             │                 │                  │
 │             │                 │─loadUserProfiles│
 │             │                 │  (userId)       │
 │             │                 │                  │
 │             │                 │<──Profiles─────│
 │             │                 │                  │
 │             │                 │─build Profile──>│
 │             │                 │  Prompt         │
 │             │                 │                  │
 │             │                 │─创建 ReActAgent─>│
 │             │                 │  (with prompt)  │
 │             │                 │                  │
 │             │<────Response───│                  │
 │<─AI 回复──│                 │                  │
 │             │                 │                  │
```

### 9.3 更新 Profile 流程 (热更新)

```
用户         Frontend        Backend             MongoDB
 │             │                 │                  │
 │─编辑文件──>│                 │                  │
 │             │                 │                  │
 │             │─PUT /profiles/│                  │
 │             │  {filename}    │                  │
 │             │                 │                  │
 │             │                 │─updateProfile─>│
 │             │                 │                  │
 │             │                 │<──保存────────│
 │             │                 │                  │
 │             │<─保存成功─────│                  │
 │<─提示成功──│                 │                  │
 │             │                 │                  │
 │─下次对话──>│                 │                  │
 │             │                 │                  │
 │             │                 │─自动加载新────>│
 │             │                 │  Profile        │
 │             │                 │  (热更新生效)   │
```

---

## 10. 异常处理与边界情况

### 10.1 异常情况

| 场景 | 处理策略 |
|------|----------|
| MongoDB 连接失败 | 降级使用 resources 默认文件，记录日志告警 |
| Profile 文件内容为空 | 跳过该文件，不报错 |
| Profile 文件不存在 (MongoDB 和 resources) | 继续加载其他文件，全部不存在则使用原有 TdAgentPromptService 的 prompt |
| 用户注册时 Profile 初始化失败 | 不阻塞注册流程，记录日志，可后续补偿 |
| 并发更新同一 Profile | 使用 MongoDB 乐观锁 (@Version)，后写入者失败并重试 |
| 非法文件名 (如 ../../etc/passwd) | 校验文件名，只允许 SOUL.md, AGENTS.md, PROFILE.md |

### 10.2 边界情况

| 场景 | 处理策略 |
|------|----------|
| 用户首次对话 (无 Profile) | 自动使用 resources 默认文件 |
| 用户更新了 Profile 但正在对话中 | 当前会话不受影响，下次创建 Agent 时生效 |
| 用户禁用了所有 Profile 文件 | 降级使用原有的 TdAgentPromptService prompt |
| Profile 文件包含非法字符 | 保存时不校验内容 (Markdown 自由格式)，由 Agent 自行处理 |
| 文件过大 (如 > 100KB) | 前端提示，后端不限制 (由 MongoDB 文档大小限制 16MB 兜底) |

---

## 11. 性能优化

### 11.1 缓存策略

**问题：** 每次创建 Agent 都查询 MongoDB 会影响性能。

**方案：** 引入本地缓存 (Caffeine)

```java
@Service
public class TdAgentProfileService {
    
    // 缓存用户 Profile，5 分钟过期
    private final Cache<String, List<AgentProfileDocument>> profileCache = 
            Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(5, TimeUnit.MINUTES)
                    .build();
    
    public List<AgentProfileDocument> loadUserProfiles(String userId) {
        return profileCache.get(userId, key -> {
            // 缓存未命中，查询 MongoDB
            return loadFromMongo(key);
        });
    }
    
    // 更新 Profile 时清除缓存
    @Transactional
    public AgentProfileDocument updateProfile(...) {
        // ... 保存逻辑
        profileCache.invalidate(userId);  // 清除缓存，热更新生效
    }
}
```

**缓存失效时机：**
- 更新 Profile 时主动清除对应用户缓存
- 5 分钟自动过期 (防止缓存无限增长)
- 最大缓存 1000 个用户

### 11.2 批量查询优化

加载用户所有 Profile 时，使用单次查询而非逐个查询：

```java
// 好的做法
List<AgentProfileDocument> profiles = 
        profileRepository.findByUserId(userId);  // 一次查询

// 避免
for (String filename : DEFAULT_FILES) {
    profileRepository.findByUserIdAndFilename(userId, filename);  // N 次查询
}
```

---

## 12. 测试策略

### 12.1 单元测试

| 测试类 | 测试要点 |
|--------|----------|
| `TdAgentProfileServiceTest` | 初始化用户、加载 Profile、更新 Profile、降级兜底 |
| `ProfilePromptBuilderTest` | 构建 prompt、剥离 frontmatter、处理 heartbeat、空文件跳过 |
| `TdAgentProfileControllerTest` | 接口参数校验、权限校验、正常流程 |

### 12.2 集成测试

| 测试场景 | 验证点 |
|----------|--------|
| 用户注册流程 | Profile 自动初始化，MongoDB 中有 3 条记录 |
| 对话流程 | System Prompt 包含用户自定义的 Profile 内容 |
| 热更新 | 更新 Profile 后，下次对话使用新内容 |
| 降级兜底 | MongoDB 无配置时，使用 resources 默认文件 |

### 12.3 端到端测试

| 场景 | 验证点 |
|------|--------|
| 前端编辑保存 | 保存成功，MongoDB 内容更新 |
| 前端刷新页面 | 加载最新 Profile |
| 前端启用/禁用开关 | 开关状态持久化 |
| 前端重置按钮 | 恢复为默认文件内容 |

---

## 13. 上线检查清单

- [ ] MongoDB 新增 `agent_profiles` collection，并创建复合唯一索引
- [ ] resources/profiles/ 下有 3 个默认文件
- [ ] 用户注册时自动初始化 Profile
- [ ] API 接口都有权限校验（只能操作自己的 Profile）
- [ ] 热更新生效（更新后下次对话使用新内容）
- [ ] 降级兜底正常（MongoDB 无配置时使用默认文件）
- [ ] 前端页面对接真实 API
- [ ] 性能测试：Profile 加载不影响对话响应时间 (P99 < 200ms)
- [ ] 日志记录：Profile 加载、更新、降级都有日志记录

---

## 14. 后续扩展

### 14.1 可能的扩展方向

1. **版本管理**：记录 Profile 的修改历史，支持回滚到历史版本
2. **模板市场**：提供预设的 Profile 模板，用户可以选择应用
3. **AI 辅助更新**：让 AI 根据对话历史自动建议 PROFILE.md 更新
4. **多 Agent 支持**：一个用户可以有多个 Agent，每个 Agent 有独立的 Profile
5. **权限控制**：管理员可以管理所有用户的 Profile，普通用户只能管理自己的

### 14.2 配置文件扩展

未来可能支持更多配置文件：

| 文件名 | 用途 |
|--------|------|
| HEARTBEAT.md | 心跳机制配置（定时任务、定期检查） |
| MEMORY.md | 长期记忆配置（用户的重要事件、决策） |
| TOOLS.md | 工具使用偏好和权限 |

当前实现应支持动态扩展，不硬编码文件名列表。

---

## 15. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| MongoDB 故障 | 无法加载用户 Profile | 降级使用 resources 默认文件 |
| Profile 内容过大 | 对话响应变慢 | 前端提示，监控性能指标 |
| 并发更新冲突 | 用户更新丢失 | 乐观锁 + 前端重试提示 |
| 安全问题 (XSS/注入) | 系统被攻击 | Profile 内容注入到 prompt，由 LLM 处理，不涉及代码执行 |
| 用户误操作 | Profile 内容损坏 | 提供重置为默认值功能 |

---

## 16. 总结

本设计实现了一个完整的 Profile System Prompt 热更新机制，核心特点：

✅ **用户隔离**：每个用户独立的 Profile 配置  
✅ **热更新**：更新后立即生效，无需重启  
✅ **降级兜底**：多层降级保证系统可用性  
✅ **性能优化**：本地缓存减少 DB 查询  
✅ **前后端一致**：API 设计与前端页面完美对接  
✅ **可扩展**：支持未来新增配置文件类型  

设计遵循了以下原则：
- **KISS** (Keep It Simple, Stupid)：简单直接的设计
- **YAGNI** (You Aren't Gonna Need It)：不提前实现不需要的功能
- **关注点分离**：Profile 管理、Prompt 构建、Agent 创建各司其职
- **防御性编程**：所有文件都是可选的，不存在不报错
