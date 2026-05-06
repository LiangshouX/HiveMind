# 云端 Skill 快速开始指南

## 前置条件

- Node.js >= 18
- Java >= 17
- MySQL >= 8.0
- 阿里云 OSS 账号（或使用 MinIO 本地开发）

## 第一步：配置后端

### 1.1 创建数据库表

```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS tang_dynasty DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

# 执行建表脚本
use tang_dynasty;
source D:\Code\Java\TangDynasty\docs\skill\schema.sql;
```

### 1.2 配置 OSS（开发环境使用 MinIO）

```bash
# 启动 MinIO（Docker）
docker run -d --name minio-dev -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=dev \
  -e MINIO_ROOT_PASSWORD=dev-secret \
  -v ~/oss-data:/data \
  minio/minio server /data --console-address ":9001"
```

访问 MinIO 控制台：http://localhost:9001
- 用户名：`dev`
- 密码：`dev-secret`

创建 Bucket：`agent-skills-dev`

### 1.3 配置 application.yml

在 `tang-dynasty-launcher/src/main/resources/application.yml` 中添加：

```yaml
td:
  skill:
    storage:
      oss:
        enabled: true
        endpoint: http://localhost:9000
        access-key-id: dev
        access-key-secret: dev-secret
        bucket-name: agent-skills-dev
        base-path: agent-skills
        default-expire-minutes: 60
```

### 1.4 启动后端

```bash
cd D:\Code\Java\TangDynasty
mvn clean package -DskipTests
java -jar tang-dynasty-launcher/target/*.jar
```

后端默认启动在：http://localhost:8080

## 第二步：配置前端

### 2.1 安装依赖

```bash
cd D:\Code\Java\TangDynasty\website
npm install
```

### 2.2 配置环境变量

创建 `.env` 文件：

```bash
# 开发环境
VITE_BACKEND_API_ROOT=/api/v1
```

如果使用 Vite 代理，需要在 `vite.config.ts` 中配置：

```typescript
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

### 2.3 启动前端

```bash
npm run dev
```

前端默认启动在：http://localhost:5173

## 第三步：使用 Skill Library

### 3.1 登录系统

1. 打开浏览器访问：http://localhost:5173
2. 使用已有账号登录，或注册新账号

### 3.2 导航到技能库

在左侧菜单中：
- 点击 **都察院** → **技能库**

### 3.3 创建第一个技能

1. 点击 **创建新技能** 按钮
2. 填写表单：
   - **技能名称**：例如 "数据分析助手"
   - **描述**：例如 "用于分析 CSV 数据并生成可视化图表"
   - **标签**：输入 `data` 然后按回车，再输入 `analysis` 按回车
   - **版本号**：`1.0.0`
   - **SKILL.md 内容**：
     ```markdown
     ---
     name: 数据分析助手
     description: 用于分析 CSV 数据并生成可视化图表
     ---
     
     # 数据分析助手
     
     这是一个数据分析技能，支持：
     - CSV 文件读取和解析
     - 数据过滤和排序
     - 生成图表（柱状图、折线图、饼图）
     - 导出结果为 Excel
     
     ## 使用方法
     
     上传 CSV 文件，选择需要分析的列，然后点击"分析"按钮。
     ```
   - **是否立即发布**：打开开关

3. 点击 **保存** 按钮

### 3.4 查看技能

创建成功后，技能会显示在列表中：
- 卡片显示技能名称、描述、版本、状态
- 状态标签显示为 **已发布**（绿色）
- 版本标签显示为 **v1.0.0**（蓝色）

### 3.5 编辑技能（创建新版本）

1. 点击技能卡片进入查看模式
2. 点击 **编辑** 按钮
3. 修改 SKILL.md 内容（例如添加新功能说明）
4. 版本号自动递增为 `1.0.1`
5. 点击 **保存**

### 3.6 下载技能

1. 点击技能卡片上的 **下载** 图标
2. 系统会获取预签名下载 URL
3. 新窗口打开下载 `skill.tar.gz` 文件
4. 解压后可以看到：
   ```
   skill.tar.gz
   ├── SKILL.md
   └── references/ (如果有)
   └── scripts/ (如果有)
   └── assets/ (如果有)
   ```

### 3.7 归档技能

1. 点击技能卡片上的 **归档** 图标
2. 确认对话框中点击 **确定**
3. 技能状态变为 **已归档**（红色）

### 3.8 删除技能

1. 点击技能卡片上的 **删除** 图标（红色）
2. 确认对话框中点击 **确定**
3. 技能从列表中移除（软删除）

## 第四步：API 测试（可选）

使用 curl 或 Postman 测试 API：

### 创建技能

```bash
curl -X POST http://localhost:8080/api/v1/tdagent/skills/cloud/users/{userId} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {your-token}" \
  -d '{
    "name": "测试技能",
    "description": "这是一个测试技能",
    "skillMarkdown": "---\nname: 测试技能\n---\n测试内容",
    "tags": ["test", "demo"],
    "version": "1.0.0",
    "publish": true
  }'
```

### 查询技能列表

```bash
curl http://localhost:8080/api/v1/tdagent/skills/cloud/page?userId={userId} \
  -H "Authorization: Bearer {your-token}"
```

### 获取下载链接

```bash
curl http://localhost:8080/api/v1/tdagent/skills/cloud/users/{userId}/{skillId}/download \
  -H "Authorization: Bearer {your-token}"
```

## 常见问题

### Q1: 前端请求 404

**原因**：后端未启动或 API 地址配置错误

**解决**：
1. 检查后端是否启动：访问 http://localhost:8080/actuator/health
2. 检查 `.env` 文件中的 `VITE_BACKEND_API_ROOT` 配置
3. 如果使用代理，检查 `vite.config.ts` 代理配置

### Q2: 创建技能失败，提示"同名技能已存在"

**原因**：该用户下已有同名技能

**解决**：使用不同的技能名称

### Q3: 下载链接无效

**原因**：OSS 未配置或文件不存在

**解决**：
1. 检查 `application.yml` 中 OSS 配置是否正确
2. 检查 MinIO 是否启动（开发环境）
3. 登录 MinIO 控制台查看文件是否存在

### Q4: 前端编译错误

**原因**：TypeScript 类型错误或依赖缺失

**解决**：
```bash
# 重新安装依赖
rm -rf node_modules package-lock.json
npm install

# 检查类型
npx tsc --noEmit
```

## 下一步

- 📖 查看完整文档：`docs/skill/Design.md`
- 🔧 配置生产环境 OSS：`docs/skill/CONFIG.md`
- 🎨 前端实现细节：`docs/skill/FRONTEND_IMPLEMENTATION.md`
- 📝 数据库表结构：`docs/skill/schema.sql`

## 技术支持

如有问题，请查看：
- 后端日志：控制台输出
- 前端日志：浏览器控制台（F12）
- API 请求：浏览器 Network 面板

祝使用愉快！ 🎉
