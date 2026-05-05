# Token Usage 功能实现说明

## 已完成功能

### 后端实现（tang-dynasty-agent-engine 模块）

#### 1. 数据库层
- **SQL 脚本**: `docs/token/td_token_usage.sql`
- **实体类**: `TokenUsagePO.java` - MyBatis Plus 实体
- **Mapper**: `TokenUsageMapper.java` - 数据访问接口
- **Support**: `TokenUsageSupport.java` + `TokenUsageSupportImpl.java` - 聚合查询实现

#### 2. 应用层
- **Service 接口**: `ITokenUsageRecordService.java`
- **Service 实现**: `TokenUsageRecordServiceImpl.java`
  - `record()` - 记录 Token 使用量
  - `getSummary()` - 汇总统计
  - `getByModel()` - 按模型统计
  - `getByProvider()` - 按供应商统计
  - `getByDate()` - 按日期统计

#### 3. API 层
- **Controller**: `TokenUsageController.java`
  - `GET /api/agent/token-usage/summary` - 汇总统计
  - `GET /api/agent/token-usage/by-model` - 按模型统计
  - `GET /api/agent/token-usage/by-provider` - 按供应商统计
  - `GET /api/agent/token-usage/by-date` - 按日期统计

#### 4. Token 采集集成
- **修改文件**: `TdAgentStreamingServiceImpl.java`
- **采集点**: AGENT_RESULT 事件处理处
- **采集逻辑**: 
  1. 从 `modelFactory.currentConfig()` 获取当前模型配置
  2. 从 `message.getMetadata()` 提取 `_chat_usage`
  3. 调用 `tokenUsageRecordService.record()` 写入 MySQL

#### 5. 依赖添加
- **pom.xml**: 添加 `spring-boot-starter-security` 依赖
- **SecurityUtils**: 创建 `com.liangshou.tangdynasty.agentic.common.util.SecurityUtils`

### 前端实现（website 模块）

#### 1. API 调用
- **文件**: `website/src/api.ts`
- **新增方法**:
  - `api.tokenUsageSummary()`
  - `api.tokenUsageByModel()`
  - `api.tokenUsageByProvider()`
  - `api.tokenUsageByDate()`

#### 2. 类型定义
- 在 `api.ts` 中添加：
  - `TokenUsageSummary`
  - `TokenUsageByModel`
  - `TokenUsageByProvider`
  - `TokenUsageByDate`

#### 3. 页面组件
- **文件**: `website/src/pages/Dalisi/TokenUsage.tsx`
- **功能**:
  - 日期范围选择器（默认最近 7 天）
  - 4 个统计卡片：总费用、总 Token、调用次数、活跃模型数
  - 按模型统计表格（含供应商、输入/输出 Token、调用次数、费用）
  - 加载状态和空数据处理

## 部署步骤

### 1. 数据库初始化

```bash
mysql -u root -p tang_dynasty < docs/token/td_token_usage.sql
```

或手动执行 SQL：

```sql
-- 在 MySQL 中执行
CREATE TABLE IF NOT EXISTS `td_token_usage` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `session_id` VARCHAR(128) NOT NULL COMMENT '会话ID',
  `message_id` VARCHAR(128) NOT NULL COMMENT '消息ID（AgentScope Msg ID）',
  `model_provider` VARCHAR(64) NOT NULL COMMENT '模型供应商（dashscope/openai）',
  `model_name` VARCHAR(128) NOT NULL COMMENT '模型名称（如 qwen-max、gpt-4o）',
  `input_tokens` INT NOT NULL DEFAULT 0 COMMENT '输入Token数',
  `output_tokens` INT NOT NULL DEFAULT 0 COMMENT '输出Token数',
  `total_tokens` INT NOT NULL DEFAULT 0 COMMENT '总Token数',
  `cached_tokens` INT DEFAULT 0 COMMENT '缓存命中Token数（可选）',
  `usage_time` DATETIME NOT NULL COMMENT '使用时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_user_time` (`user_id`, `usage_time`),
  INDEX `idx_user_model` (`user_id`, `model_name`),
  INDEX `idx_user_provider` (`user_id`, `model_provider`),
  INDEX `idx_session` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent Token使用统计表';
```

### 2. 后端部署

```bash
# 编译
cd D:\Code\Java\TangDynasty
mvn clean package -pl tang-dynasty-agent-engine -am

# 重启服务
# (根据你的启动方式)
```

### 3. 前端部署

```bash
cd D:\Code\Java\TangDynasty\website
npm run build  # 或你的构建命令
```

## 验证方法

### 1. 后端验证

**步骤 1**: 发起一次 Agent 对话（通过前端或 API）

**步骤 2**: 检查 MySQL 是否有记录：

```sql
SELECT * FROM td_token_usage ORDER BY created_at DESC LIMIT 10;
```

**预期结果**: 应该有对应模型、Token 用量的记录。

**步骤 3**: 测试查询 API：

```bash
# 替换 YOUR_TOKEN 为实际的 JWT Token
curl -X GET "http://localhost:8080/api/agent/token-usage/summary?startDate=2026-05-01&endDate=2026-05-05" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**预期结果**: 返回 JSON 格式的汇总统计数据。

### 2. 前端验证

**步骤 1**: 访问前端页面，导航到 `/token-usage` 路由

**步骤 2**: 检查页面显示：
- 日期选择器默认为最近 7 天
- 统计卡片显示：总费用、总 Token、调用次数、活跃模型数
- 表格显示按模型统计的数据

**步骤 3**: 切换日期范围，验证数据刷新

## 数据流说明

```
用户发起对话
  ↓
TdAgentStreamingServiceImpl.stream()
  ↓
Agent.execute() → 生成最终回复
  ↓
收到 AGENT_RESULT 事件
  ↓
recordTokenUsage() 方法被调用
  ↓
从 modelFactory.currentConfig() 获取模型信息
从 message.getMetadata() 提取 _chat_usage
  ↓
TokenUsageRecordService.record()
  ↓
解析 metadata 和 modelConfig
  ↓
TokenUsageSupport.save() → 写入 MySQL td_token_usage 表
```

## 注意事项

### 1. 用户ID获取
- 使用 `SecurityUtils.getCurrentUserId()` 从 Spring Security 上下文获取
- 确保请求已通过认证（JWT Token 验证）

### 2. 元数据格式
- AgentScope 框架在 `metadata._chat_usage` 中记录用量
- 格式：`{inputTokens, outputTokens, totalTokens, time}`
- 如果 metadata 缺失 `_chat_usage`，则跳过记录（不影响主流程）

### 3. 费用计算
- 当前 `estimatedCost` 字段返回 0（占位符）
- 后续可添加模型单价配置实现费用估算：
  - 方案 1：配置文件 `application.yaml` 中添加 `td.agent.token-pricing`
  - 方案 2：创建 `td_model_pricing` 数据库表

### 4. 错误处理
- Token 记录失败仅记录 WARN 日志，不影响对话流程
- 使用 try-catch 包裹，确保异常不传播

## 扩展计划

### 短期（可选功能）
1. **费用估算**：添加模型单价配置，实现真实费用计算
2. **按会话统计**：添加 `/by-session` API，查看单个 Session 的用量
3. **导出功能**：支持导出 CSV/Excel 报表

### 长期（性能优化）
1. **批量插入**：高并发场景下改为批量写入
2. **分区表**：按月份分区 `td_token_usage_202605`
3. **缓存聚合**：Redis 缓存常用统计数据

## 文件清单

### 后端新增/修改文件
```
tang-dynasty-agent-engine/
├── pom.xml (修改：添加 spring-boot-starter-security)
├── src/main/java/com/liangshou/tangdynasty/agentic/
│   ├── adapter/controller/
│   │   └── TokenUsageController.java (新增)
│   ├── application/
│   │   ├── ITokenUsageRecordService.java (新增)
│   │   └── impl/
│   │       └── TokenUsageRecordServiceImpl.java (新增)
│   ├── common/util/
│   │   └── SecurityUtils.java (新增)
│   ├── infrastructure/mysql/
│   │   ├── mapper/
│   │   │   └── TokenUsageMapper.java (新增)
│   │   ├── po/
│   │   │   └── TokenUsagePO.java (新增)
│   │   └── support/
│   │       ├── TokenUsageSupport.java (新增)
│   │       └── impl/
│   │           └── TokenUsageSupportImpl.java (新增)
│   └── application/impl/
│       └── TdAgentStreamingServiceImpl.java (修改：集成 Token 采集)
```

### 文档
```
docs/token/
├── Design.md (设计文档)
├── td_token_usage.sql (数据库脚本)
└── README.md (本文件)
```

### 前端修改文件
```
website/src/
├── api.ts (添加 API 方法和类型定义)
└── pages/Dalisi/
    └── TokenUsage.tsx (更新为真实 API 调用)
```

## 技术栈

- **后端**: Spring Boot 4.0.3, MyBatis Plus 3.5.15, MySQL 8.0
- **前端**: React 18, TypeScript, Ant Design 5, Axios, Day.js
- **框架**: AgentScope 1.0.12

## 作者

LiangshouX - 2026-05-05
