# TangDynasty Backend - RESTful API 规范

> 本文档基于前端已上线的交互行为与数据结构反推并重构。

## 全局规范

1. **版本控制**：所有接口前缀统一为 `/api/v1`
2. **鉴权方式**：`Authorization: Bearer <JWT_TOKEN>`
3. **TraceId追踪**：请求响应头均包含 `X-Trace-Id`
4. **统一响应格式**：
```json
{
  "code": 200,
  "msg": "success",
  "data": { ... }
}
```

## 领域模型与接口映射

### 1. 认证领域 (Auth)
- `POST /api/v1/auth/login`: 登录并获取JWT

### 2. 旨意任务领域 (Tasks)
- `GET /api/v1/tasks`: 获取任务列表（支持分页、按状态过滤）
- `POST /api/v1/tasks`: 创建新任务（皇上下旨）
- `GET /api/v1/tasks/{id}`: 获取任务详情
- `GET /api/v1/tasks/{id}/activity`: 获取任务动态日志
- `POST /api/v1/tasks/{id}/action`: 对任务执行操作（如取消、重试、归档）
- `PUT /api/v1/tasks/{id}/state`: 推进任务状态（流转）

### 3. 官员与组织领域 (Officials/Agents)
- `GET /api/v1/officials`: 获取官员列表与统计数据
- `GET /api/v1/officials/status`: 获取官员实时运行状态
- `PUT /api/v1/officials/{agentId}/model`: 设置官员使用的模型
- `POST /api/v1/officials/{agentId}/wake`: 唤醒官员

### 4. 系统与配置领域 (System/Config)
- `GET /api/v1/system/live-status`: 大盘实时状态（LiveStatus）
- `GET /api/v1/system/morning-brief`: 获取早朝奏折简报
- `POST /api/v1/system/morning-brief/refresh`: 刷新早朝简报
- `GET /api/v1/system/model-change-log`: 模型变更日志

### 5. 技能与工具领域 (Skills)
- `GET /api/v1/skills`: 获取所有技能列表
- `POST /api/v1/skills`: 注册新技能
- `GET /api/v1/skills/{agentId}/{skillName}`: 获取技能详情
- `DELETE /api/v1/skills/{agentId}/{skillName}`: 移除技能

## 错误码规范
- `200`: 成功
- `400`: 参数校验失败（结合 JSR-380）
- `401`: 未认证或 Token 过期
- `403`: 权限不足（越权访问拦截）
- `404`: 资源不存在
- `500`: 系统内部异常（返回 TraceId 供排查）
