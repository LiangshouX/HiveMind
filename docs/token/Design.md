# Token Usage 统计功能设计文档

## 一、需求概述

为 Agent 平台添加 Token 使用量记录功能，支持：
1. **按登录用户区分**：每个用户只能查看自己的 Token 使用数据
2. **按模型统计**：查看不同模型（如 gpt-4o、claude-3-5-sonnet、qwen-max）的用量
3. **按供应商统计**：查看不同供应商（如 dashscope、openai）的用量
4. **按时间范围查询**：支持日期范围过滤
5. **费用估算**：根据模型单价估算费用（可选）

数据存储使用 MySQL，通过 MyBatis Plus 实现 CRUD。

## 二、现有架构分析

### 2.1 Token 数据来源

AgentScope 框架在 LLM 调用完成后，会在 `Msg.metadata` 中记录用量信息，格式如下：

```json
{
  "_chat_usage": {
    "inputTokens": 2859,
    "outputTokens": 328,
    "time": 25.871,
    "totalTokens": 3187
  }
}
```

该 metadata 存在于：
- **AGENT_RESULT 事件**：Agent 最终回复消息的 metadata 字段
- **MongoDB 消息记录**：`conversations_memory` 集合中 `StoredMessage.metadata` 字段

### 2.2 现有 TokenUsage 表（backend 模块）

`backend` 模块已有 `sys_token_usage` 表，但该表用于传统的任务调用场景，字段包括：
- `user_id`, `model_provider_id`, `model_id`, `official_id`
- `prompt_tokens`, `completion_tokens`, `total_tokens`, `cached_prompt_tokens`
- `record_time`

**设计决策**：在 `agent-engine` 模块创建独立的 `td_token_usage` 表，原因：
1. Agent 引擎的模型配置与传统任务不同（使用 `modelConfig` JSON）
2. 需要记录 Agent 特有的维度（sessionId、agentName）
3. 避免与 backend 模块耦合

### 2.3 CoPaw 参考设计（Python）

CoPaw 的 Token Usage 功能特点：
- **装饰器模式自动采集**：包装 ChatModel，拦截响应提取 usage
- **多维度聚合**：total / by_model / by_provider / by_date
- **本地 JSON 存储**：按日期→provider:model 两级 key 组织

**Java 项目适配**：
- 不使用装饰器模式，改为在**事件流处理环节采集**（已有 metadata）
- 使用 MySQL 而非 JSON 文件存储
- 提供 RESTful API 供前端查询

## 三、数据库设计

### 3.1 表结构：`td_token_usage`

```sql
CREATE TABLE `td_token_usage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `session_id` VARCHAR(128) NOT NULL COMMENT '会话ID',
  `message_id` VARCHAR(128) NOT NULL COMMENT '消息ID（AgentScope Msg ID）',
  
  -- 模型信息
  `model_provider` VARCHAR(64) NOT NULL COMMENT '模型供应商（dashscope/openai）',
  `model_name` VARCHAR(128) NOT NULL COMMENT '模型名称（如 qwen-max、gpt-4o）',
  
  -- Token 用量
  `input_tokens` INT NOT NULL DEFAULT 0 COMMENT '输入Token数',
  `output_tokens` INT NOT NULL DEFAULT 0 COMMENT '输出Token数',
  `total_tokens` INT NOT NULL DEFAULT 0 COMMENT '总Token数',
  `cached_tokens` INT DEFAULT 0 COMMENT '缓存命中Token数（可选）',
  
  -- 时间信息
  `usage_time` DATETIME NOT NULL COMMENT '使用时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  
  PRIMARY KEY (`id`),
  INDEX `idx_user_time` (`user_id`, `usage_time`),
  INDEX `idx_user_model` (`user_id`, `model_name`),
  INDEX `idx_user_provider` (`user_id`, `model_provider`),
  INDEX `idx_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent Token使用统计表';
```

### 3.2 索引策略

| 索引名 | 字段 | 用途 |
|--------|------|------|
| `idx_user_time` | (user_id, usage_time) | 按用户+时间范围查询 |
| `idx_user_model` | (user_id, model_name) | 按用户+模型统计 |
| `idx_user_provider` | (user_id, model_provider) | 按用户+供应商统计 |
| `idx_session` | (session_id) | 按会话查询（可选功能） |

## 四、架构设计

### 4.1 分层架构

```
tang-dynasty-agent-engine/
├── infrastructure/mysql/
│   ├── po/
│   │   └── TokenUsagePO.java              # MyBatis Plus 实体
│   ├── mapper/
│   │   └── TokenUsageMapper.java          # Mapper 接口
│   └── support/
│       ├── ITokenUsageSupport.java        # Support 接口
│       └── impl/TokenUsageSupportImpl.java # Support 实现
│
├── application/
│   ├── ITokenUsageRecordService.java      # 记录服务接口
│   └── impl/
│       └── TokenUsageRecordServiceImpl.java # 记录+查询服务
│
└── adapter/controller/
    └── TokenUsageController.java          # REST API
```

### 4.2 核心流程

#### 4.2.1 Token 采集流程

```
Agent.stream() 
  → Flux<Event> 订阅
    → 收到 AGENT_RESULT 事件
      → 提取 Msg.metadata._chat_usage
        → 调用 TokenUsageRecordService.record()
          → 构建 TokenUsagePO
            → MyBatis Plus 插入 MySQL
```

**采集点选择**：在 `TdAgentStreamingServiceImpl.execute()` 方法的 `AGENT_RESULT` 事件处理处采集。

**原因**：
1. AGENT_RESULT 是最终结果事件，包含完整的 metadata
2. 此时已知道完整的会话上下文（userId、sessionId）
3. 不影响流式输出的性能（异步记录）

#### 4.2.2 模型信息获取

问题：AGENT_RESULT 事件的 Msg 对象中**没有直接包含模型名称和供应商信息**。

**解决方案**：从 `ConversationSessionContext` 获取模型配置

```java
// ConversationSessionContext 已包含 modelConfig
context.getModelConfig() 
  → 解析 JSON 获取 provider 和 model name
```

`modelConfig` 结构示例：
```json
{
  "provider": "dashscope",
  "modelName": "qwen-max",
  "apiKey": "...",
  "baseUrl": "...",
  "enableThinking": true
}
```

### 4.3 类设计

#### 4.3.1 TokenUsagePO

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("td_token_usage")
public class TokenUsagePO implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("user_id")
    private String userId;
    
    @TableField("session_id")
    private String sessionId;
    
    @TableField("message_id")
    private String messageId;
    
    @TableField("model_provider")
    private String modelProvider;
    
    @TableField("model_name")
    private String modelName;
    
    @TableField("input_tokens")
    private Integer inputTokens;
    
    @TableField("output_tokens")
    private Integer outputTokens;
    
    @TableField("total_tokens")
    private Integer totalTokens;
    
    @TableField("cached_tokens")
    private Integer cachedTokens;
    
    @TableField("usage_time")
    private LocalDateTime usageTime;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

#### 4.3.2 TokenUsageRecordService

```java
public interface ITokenUsageRecordService {
    /**
     * 记录 Token 使用量
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param messageId 消息ID
     * @param modelConfig 模型配置 JSON
     * @param metadata 消息 metadata（包含 _chat_usage）
     */
    void record(String userId, String sessionId, String messageId, 
                String modelConfig, Map<String, Object> metadata);
    
    /**
     * 按用户和时间范围查询汇总统计
     * @param userId 用户ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 汇总统计
     */
    TokenUsageSummaryVO getSummary(String userId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 按模型分组统计
     */
    List<TokenUsageByModelVO> getByModel(String userId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 按供应商分组统计
     */
    List<TokenUsageByProviderVO> getByProvider(String userId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 按日期分组统计（趋势）
     */
    List<TokenUsageByDateVO> getByDate(String userId, LocalDate startDate, LocalDate endDate);
}
```

#### 4.3.3 VO 对象设计

**TokenUsageSummaryVO**（汇总统计）：
```java
@Data
public class TokenUsageSummaryVO {
    private Long totalTokens;
    private Long totalInputTokens;
    private Long totalOutputTokens;
    private Long totalCalls;
    private BigDecimal estimatedCost;  // 可选，根据单价计算
    private LocalDate startDate;
    private LocalDate endDate;
}
```

**TokenUsageByModelVO**（按模型统计）：
```java
@Data
public class TokenUsageByModelVO {
    private String modelName;
    private String modelProvider;
    private Long totalTokens;
    private Long inputTokens;
    private Long outputTokens;
    private Long callCount;
    private BigDecimal estimatedCost;
}
```

**TokenUsageByProviderVO**（按供应商统计）：
```java
@Data
public class TokenUsageByProviderVO {
    private String modelProvider;
    private Long totalTokens;
    private Long inputTokens;
    private Long outputTokens;
    private Long callCount;
    private Long modelCount;  // 使用的模型数量
}
```

**TokenUsageByDateVO**（按日期统计 - 用于趋势图）：
```java
@Data
public class TokenUsageByDateVO {
    private LocalDate usageDate;
    private Long totalTokens;
    private Long inputTokens;
    private Long outputTokens;
    private Long callCount;
}
```

### 4.4 API 设计

#### 4.4.1 Controller: TokenUsageController

```java
@RestController
@RequestMapping("/api/agent/token-usage")
@RequiredArgsConstructor
public class TokenUsageController {
    
    private final ITokenUsageRecordService tokenUsageService;
    
    // 获取汇总统计
    @GetMapping("/summary")
    public Result<TokenUsageSummaryVO> getSummary(
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate) {
        String userId = SecurityUtils.getCurrentUserId();
        return Result.success(tokenUsageService.getSummary(userId, startDate, endDate));
    }
    
    // 按模型统计
    @GetMapping("/by-model")
    public Result<List<TokenUsageByModelVO>> getByModel(
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate) {
        String userId = SecurityUtils.getCurrentUserId();
        return Result.success(tokenUsageService.getByModel(userId, startDate, endDate));
    }
    
    // 按供应商统计
    @GetMapping("/by-provider")
    public Result<List<TokenUsageByProviderVO>> getByProvider(
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate) {
        String userId = SecurityUtils.getCurrentUserId();
        return Result.success(tokenUsageService.getByProvider(userId, startDate, endDate));
    }
    
    // 按日期统计（趋势）
    @GetMapping("/by-date")
    public Result<List<TokenUsageByDateVO>> getByDate(
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate endDate) {
        String userId = SecurityUtils.getCurrentUserId();
        return Result.success(tokenUsageService.getByDate(userId, startDate, endDate));
    }
}
```

**API 路径选择**：`/api/agent/token-usage` 而非 `/api/sys-token-usages`，原因：
1. 区分 Agent 引擎和传统 backend 模块
2. 语义更清晰（agent 前缀）
3. 避免与现有 SysTokenUsageController 冲突

#### 4.4.2 前端对接

前端路由：`/token-usage`
前端文件：`website/src/pages/Dalisi/TokenUsage.tsx`

**现有前端功能**：
- 日期范围选择器（RangePicker）
- 统计卡片：总费用、总 Token、活跃模型数
- 表格：按模型展示 input/output/total Tokens、费用

**需要修改**：
1. 替换 mockData 为真实 API 调用
2. 调用 `/api/agent/token-usage/by-model` 获取按模型统计数据
3. 调用 `/api/agent/token-usage/summary` 获取汇总数据
4. 添加按供应商统计的展示（可选，新增 Tab 或卡片）

**前端 API 调用示例**：
```typescript
// website/src/api/tokenUsage.ts
import request from '@/utils/request';

export const getTokenUsageSummary = (startDate: string, endDate: string) => {
  return request.get('/api/agent/token-usage/summary', {
    params: { startDate, endDate }
  });
};

export const getTokenUsageByModel = (startDate: string, endDate: string) => {
  return request.get('/api/agent/token-usage/by-model', {
    params: { startDate, endDate }
  });
};
```

## 五、实现细节

### 5.1 Token 采集实现

在 `TdAgentStreamingServiceImpl.execute()` 中：

```java
eventFlux.subscribe(
    event -> {
        // ... 现有逻辑 ...
        
        if (event.getType() == EventType.AGENT_RESULT && event.getMessage() != null) {
            finalMessage.set(event.getMessage());
            
            // 新增：记录 Token 使用量
            tokenUsageRecordService.record(
                context.getUserId(),
                context.getSessionId(),
                event.getMessage().getId(),
                context.getModelConfig(),  // 需要从 context 获取
                event.getMessage().getMetadata()
            );
        }
        
        // ... 现有逻辑 ...
    },
    // ... error/complete handlers ...
);
```

### 5.2 metadata 解析逻辑

```java
private ChatUsage extractChatUsage(Map<String, Object> metadata) {
    if (metadata == null || !metadata.containsKey("_chat_usage")) {
        return null;
    }
    
    @SuppressWarnings("unchecked")
    Map<String, Object> usage = (Map<String, Object>) metadata.get("_chat_usage");
    
    return ChatUsage.builder()
        .inputTokens(getIntValue(usage, "inputTokens", 0))
        .outputTokens(getIntValue(usage, "outputTokens", 0))
        .totalTokens(getIntValue(usage, "totalTokens", 0))
        .build();
}
```

### 5.3 模型配置解析

```java
private ModelInfo parseModelConfig(String modelConfigJson) {
    try {
        JsonNode node = objectMapper.readTree(modelConfigJson);
        return ModelInfo.builder()
            .provider(node.get("provider").asText())
            .modelName(node.get("modelName").asText())
            .build();
    } catch (JsonProcessingException e) {
        log.warn("Failed to parse model config: {}", e.getMessage());
        return ModelInfo.builder()
            .provider("unknown")
            .modelName("unknown")
            .build();
    }
}
```

### 5.4 聚合查询实现

使用 MyBatis Plus 的 `QueryWrapper` 或原生 SQL 实现聚合：

```java
// 按模型统计示例
public List<TokenUsageByModelVO> getByModel(String userId, LocalDate start, LocalDate end) {
    return support.getBaseMapper().selectList(
        new QueryWrapper<TokenUsagePO>()
            .select("model_name", "model_provider", 
                    "SUM(input_tokens) as inputTokens",
                    "SUM(output_tokens) as outputTokens",
                    "SUM(total_tokens) as totalTokens",
                    "COUNT(*) as callCount")
            .eq("user_id", userId)
            .between("usage_time", start.atStartOfDay(), end.plusDays(1).atStartOfDay())
            .groupBy("model_name", "model_provider")
    );
}
```

## 六、费用估算（可选功能）

### 6.1 模型单价配置

创建配置类或数据库表存储模型单价：

**方案 1：配置文件**（推荐初期使用）
```yaml
td:
  agent:
    token-pricing:
      dashscope:
        qwen-max:
          input: 0.004  # 每 1K tokens 价格（USD）
          output: 0.012
        qwen-plus:
          input: 0.0008
          output: 0.002
      openai:
        gpt-4o:
          input: 0.005
          output: 0.015
        gpt-4o-mini:
          input: 0.00015
          output: 0.0006
```

**方案 2：数据库表**（推荐后期使用）
```sql
CREATE TABLE `td_model_pricing` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `model_provider` VARCHAR(64) NOT NULL,
  `model_name` VARCHAR(128) NOT NULL,
  `input_price_per_1k` DECIMAL(10,6) NOT NULL COMMENT '每 1K input tokens 价格',
  `output_price_per_1k` DECIMAL(10,6) NOT NULL COMMENT '每 1K output tokens 价格',
  `currency` VARCHAR(8) DEFAULT 'USD',
  `effective_date` DATE NOT NULL COMMENT '生效日期',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_provider_model_date` (`model_provider`, `model_name`, `effective_date`)
);
```

### 6.2 费用计算逻辑

```java
private BigDecimal calculateEstimatedCost(
        String provider, String model, int inputTokens, int outputTokens) {
    PricingConfig pricing = pricingCache.get(provider + ":" + model);
    if (pricing == null) {
        return BigDecimal.ZERO;
    }
    
    BigDecimal inputCost = BigDecimal.valueOf(inputTokens / 1000.0)
        .multiply(pricing.getInputPricePer1k());
    BigDecimal outputCost = BigDecimal.valueOf(outputTokens / 1000.0)
        .multiply(pricing.getOutputPricePer1k());
    
    return inputCost.add(outputCost).setScale(4, RoundingMode.HALF_UP);
}
```

## 七、前后端联调方案

### 7.1 联调步骤

1. **后端开发完成**：
   - 创建数据库表（Flyway 或手动执行 SQL）
   - 实现 PO、Mapper、Support、Service、Controller
   - 集成到流式事件处理

2. **后端自测**：
   - 使用 Postman 或 curl 测试 API
   - 验证数据是否正确写入 MySQL
   - 验证聚合查询结果正确性

3. **前端对接**：
   - 更新 `TokenUsage.tsx` 替换 mockData
   - 添加日期选择器联动
   - 处理加载状态和错误提示

4. **联调验证**：
   - 发起 Agent 对话，验证数据是否记录
   - 刷新前端页面，验证统计数据正确显示
   - 测试不同日期范围、不同模型的筛选

### 7.2 前端数据流

```
TokenUsage.tsx (组件)
  ↓ useState: summary, byModel, loading, dateRange
  ↓ useEffect: 监听 dateRange 变化
  ↓ 调用 API
  ↓ getTokenUsageSummary(startDate, endDate)
  ↓ getTokenUsageByModel(startDate, endDate)
  ↓ 更新 state
  ↓ 渲染 Statistic 卡片 + Table
```

### 7.3 前端修改清单

**TokenUsage.tsx 需要修改的部分**：

1. 移除 mockData，改为从 API 获取
2. 添加 API 调用逻辑（useEffect + axios）
3. 日期选择器 onChange 触发重新查询
4. 添加 loading 状态处理
5. 费用字段如果没有单价配置则显示 "-"

## 八、测试策略

### 8.1 单元测试

- `TokenUsageRecordServiceImplTest`：
  - 测试 record() 方法正确解析 metadata 并插入数据库
  - 测试 metadata 缺失 _chat_usage 时不记录
  - 测试 modelConfig 解析异常处理

- `TokenUsageSupportImplTest`：
  - 测试聚合查询的 SQL 生成
  - 测试日期范围过滤

### 8.2 集成测试

- 发起真实 Agent 对话，验证：
  - MySQL 中有对应记录
  - 字段值与 MongoDB metadata 一致
  - 聚合查询结果正确

### 8.3 前端测试

- Mock API 响应，验证：
  - 表格渲染正确
  - 统计卡片数值计算正确
  - 日期切换后数据刷新

## 九、扩展性考虑

### 9.1 未来可能的需求

1. **按会话统计**：查看单个 Session 的 Token 用量
   - 已有 session_id 字段，直接添加 API 即可

2. **实时统计**：WebSocket 推送实时用量
   - 可在 record() 后发送 WebSocket 消息

3. **预算告警**：超过阈值时通知用户
   - 需要新增配置表和定时任务

4. **导出报表**：导出 CSV/Excel
   - 可复用查询逻辑添加导出接口

### 9.2 性能优化

1. **批量插入**：如果并发量大，可改为批量写入
2. **分区表**：按月份分区 `td_token_usage_202605`
3. **缓存聚合结果**：Redis 缓存常用统计

## 十、风险和注意事项

### 10.1 数据一致性

- **问题**：AGENT_RESULT 事件可能因为异常而未触发
- **解决**：在 `cleanup()` 方法中兜底检查

### 10.2 metadata 格式变化

- **问题**：AgentScope 框架升级可能改变 metadata 结构
- **解决**：
  - 添加防御性检查（metadata == null || !_chat_usage 存在）
  - 日志记录异常情况但不影响主流程

### 10.3 用户权限

- **问题**：API 必须确保用户只能查看自己的数据
- **解决**：
  - 所有查询都使用 `SecurityUtils.getCurrentUserId()`
  - 不在 API 参数中传递 userId（防止伪造）

### 10.4 时区处理

- **问题**：前端日期选择器可能有时区问题
- **解决**：
  - 数据库使用 `DATETIME`（不带时区）
  - 前后端都使用 UTC 时间，前端显示时转换

## 十一、开发计划

| 阶段 | 任务 | 预计工作量 |
|------|------|-----------|
| 1 | 数据库表创建 + PO/Mapper | 0.5h |
| 2 | TokenUsageRecordService 实现 | 1h |
| 3 | 集成到流式事件处理 | 0.5h |
| 4 | 查询 API + 聚合统计 | 1h |
| 5 | 前端对接 | 1h |
| 6 | 测试验证 | 0.5h |
| **总计** | | **4.5h** |
