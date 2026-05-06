# 云端 Skill Library 前端实现

## 概述

本目录包含了云端 Skill 管理的前端实现，与后端的阿里云 OSS 存储和 MySQL 元数据管理完全对接。

## 文件结构

```
website/src/
├── types/
│   └── skillApi.ts              # Skill API 类型定义
├── services/
│   └── skillApi.ts              # Skill API 服务层
├── pages/
│   └── Censorate/
│       └── SkillLibrary.tsx     # Skill 管理页面组件
└── providers/
    └── AuthProvider.tsx         # 认证上下文（提供 userId）
```

## 功能特性

### 1. 完整的 CRUD 操作

- ✅ **创建 Skill**：支持指定版本号、标签、是否立即发布
- ✅ **查看 Skill**：浏览技能列表、查看详情、查看版本信息
- ✅ **更新 Skill**：创建新版本，支持版本号自动递增
- ✅ **删除 Skill**：软删除，支持确认对话框

### 2. 版本管理

- ✅ 版本号格式：`X.Y.Z`（例如：1.0.0, 2.1.3）
- ✅ 版本历史记录（通过 fileManifest 字段）
- ✅ 版本切换和回滚支持

### 3. 发布流程

- ✅ **草稿** → **已发布** → **已归档** 状态流转
- ✅ 一键发布技能
- ✅ 归档/下架技能

### 4. 文件下载

- ✅ 预签名 URL 下载（安全、临时有效）
- ✅ 支持 CDN 加速（如果配置）
- ✅ 一键下载完整技能包（tar.gz 格式）

### 5. 用户界面

- ✅ 响应式网格布局（适配移动端到桌面端）
- ✅ 卡片式展示，悬停效果
- ✅ 状态标签（草稿/已发布/已归档）
- ✅ 版本标签显示
- ✅ 标签系统（支持多标签筛选）
- ✅ 加载状态和空状态处理
- ✅ 确认对话框（防止误操作）
- ✅ 实时消息提示

## API 对接

### 后端端点

所有 API 请求都通过 `services/skillApi.ts` 封装，调用以下端点：

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/api/v1/tdagent/skills/cloud/page` | 分页查询 |
| GET | `/api/v1/tdagent/skills/cloud/users/{userId}/{skillId}` | 获取详情 |
| POST | `/api/v1/tdagent/skills/cloud/users/{userId}` | 创建 |
| PUT | `/api/v1/tdagent/skills/cloud/users/{userId}/{skillId}/versions` | 更新 |
| POST | `/api/v1/tdagent/skills/cloud/users/{userId}/{skillId}/publish` | 发布 |
| POST | `/api/v1/tdagent/skills/cloud/users/{userId}/{skillId}/archive` | 归档 |
| DELETE | `/api/v1/tdagent/skills/cloud/users/{userId}/{skillId}` | 删除 |
| GET | `/api/v1/tdagent/skills/cloud/users/{userId}/{skillId}/download` | 下载链接 |

### 请求流程

```
用户操作 → 组件 → skillApi.ts → http.ts → 后端 API → 响应处理 → 状态更新 → UI 刷新
```

### 错误处理

- ✅ 401 自动清除认证并跳转登录
- ✅ 403 显示无权限提示
- ✅ 500 显示服务器异常提示
- ✅ 网络超时显示错误提示
- ✅ 表单验证错误高亮显示

## 使用方式

### 1. 环境配置

确保 `.env` 文件配置正确：

```bash
# 开发环境
VITE_BACKEND_API_ROOT=/api/v1

# 生产环境（根据实际情况修改）
VITE_BACKEND_API_ROOT=https://your-domain.com/api/v1
```

### 2. 启动开发服务器

```bash
cd website
npm install  # 首次需要安装依赖
npm run dev
```

### 3. 访问页面

打开浏览器访问：`http://localhost:5173`

导航到：**都察院 → 技能库**

## 组件说明

### SkillLibrary 组件

主要的页面组件，包含：

- **状态管理**：使用 React useState 和 useCallback
- **数据加载**：useEffect 自动加载，支持手动刷新
- **表单处理**：Ant Design Form 验证和提交
- **抽屉交互**：查看/编辑/创建模式切换
- **消息提示**：成功/错误/警告消息

### 状态流转

```
创建 → 草稿 ──发布──→ 已发布 ──归档──→ 已归档
         │                              │
         └──────── 编辑 ────────────────┘
```

## 数据类型

### CloudSkill

```typescript
interface CloudSkill {
  skillId: string;              // 技能唯一标识
  userId: string;               // 用户 ID
  name: string;                 // 技能名称
  description: string;          // 技能描述
  currentVersion: string;       // 当前版本（X.Y.Z）
  status: string;               // 状态（draft/published/deprecated/archived）
  tags: string[];               // 标签列表
  dependencies: object | null;  // 依赖配置
  executionEnv: object | null;  // 运行环境
  fileManifest: object | null;  // 文件清单
  downloadUrl?: string;         // 下载 URL（可选）
  createdAt: string;            // 创建时间
  updatedAt: string;            // 更新时间
}
```

### SkillCreateRequest

```typescript
interface SkillCreateRequest {
  name: string;                 // 技能名称（必填）
  description?: string;         // 描述
  skillMarkdown: string;        // SKILL.md 内容（必填）
  resources?: object;           // 资源文件映射
  tags?: string[];              // 标签
  executionEnv?: object;        // 运行环境
  dependencies?: object;        // 依赖
  publish?: boolean;            // 是否立即发布
  version?: string;             // 版本号（默认 1.0.0）
}
```

## 安全考虑

### 1. 认证集成

- 所有 API 请求自动携带 JWT Token
- Token 失效自动清除并跳转登录
- 用户隔离（只能操作自己的技能）

### 2. 输入验证

- 前端表单验证（必填、格式检查）
- 后端二次验证（版本号格式、同名检查）
- XSS 防护（React 自动转义）

### 3. 操作确认

- 删除操作需要确认
- 归档操作需要确认
- 危险操作使用红色按钮

## 后续优化建议

1. **Markdown 预览**：集成 react-markdown 实现实时预览
2. **文件上传**：支持拖拽上传资源文件
3. **版本对比**：显示不同版本之间的差异
4. **批量操作**：支持批量发布/归档/删除
5. **搜索优化**：添加关键词搜索和标签筛选面板
6. **导入导出**：支持 JSON 格式导入导出技能配置
7. **CDN 图片**：支持技能中使用外部图片资源
8. **协作分享**：支持公开技能分享给其他用户

## 故障排除

### 问题：加载技能列表失败

**原因**：
- 后端服务未启动
- 网络配置错误
- 认证 Token 失效

**解决**：
1. 检查后端服务是否正常运行
2. 检查 `.env` 配置是否正确
3. 清除浏览器缓存并重新登录

### 问题：创建技能失败

**原因**：
- 同名技能已存在
- 版本号格式错误
- SKILL.md 内容为空

**解决**：
1. 使用不同的技能名称
2. 版本号必须为 X.Y.Z 格式
3. 确保填写了 SKILL.md 内容

### 问题：下载链接无效

**原因**：
- OSS 配置未启用
- 预签名 URL 已过期
- 文件不存在

**解决**：
1. 检查后端 OSS 配置是否启用
2. 刷新页面重新获取链接
3. 确认技能文件已正确上传

## 开发指南

### 添加新功能

1. 在 `types/skillApi.ts` 中定义类型
2. 在 `services/skillApi.ts` 中添加 API 方法
3. 在 `SkillLibrary.tsx` 中集成到组件
4. 测试并更新文档

### 调试技巧

```javascript
// 在组件中添加调试日志
console.log('当前技能列表:', skills);
console.log('用户信息:', user);

// 使用 React DevTools 检查组件状态
// 使用 Network 面板查看 API 请求
```

### 性能优化

- 使用 `useCallback` 缓存函数
- 使用 `useMemo` 缓存计算结果
- 避免不必要的重渲染
- 使用 React.lazy 实现代码分割

## 许可

本项目遵循项目根目录的 LICENSE 文件中的许可条款。
