# 云端 Skill 存储配置示例

## application.yml 配置

```yaml
# 云端 Skill OSS 存储配置
td:
  skill:
    storage:
      oss:
        # 是否启用 OSS 存储（默认 false，使用 MongoDB 本地存储）
        enabled: true
        
        # OSS 端点（生产环境建议使用内网端点）
        endpoint: https://oss-cn-hangzhou-internal.aliyuncs.com
        
        # AccessKey 配置（建议通过配置中心或环境变量注入）
        access-key-id: ${OSS_ACCESS_KEY_ID:LTAI5t...}
        access-key-secret: ${OSS_ACCESS_KEY_SECRET:xxxx...}
        
        # Bucket 名称
        bucket-name: agent-skills-prod
        
        # 根路径前缀
        base-path: agent-skills
        
        # 预签名 URL 过期时间（分钟）
        default-expire-minutes: 30
        
        # STS 配置（可选，用于前端直传）
        sts-endpoint: sts.cn-hangzhou.aliyuncs.com
        sts-role-arn: acs:ram::123456789:role/oss-upload-role
        
        # CDN 域名（可选，用于加速下载）
        cdn-domain: https://cdn.example.com
```

## 本地开发环境（MinIO）

本地开发可以使用 MinIO 模拟 OSS 服务：

```yaml
# application-dev.yml
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

启动 MinIO：

```bash
docker run -d --name minio-dev -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=dev \
  -e MINIO_ROOT_PASSWORD=dev-secret \
  -v ~/oss-data:/data \
  minio/minio server /data --console-address ":9001"
```

访问 MinIO 控制台：http://localhost:9001

## 环境变量配置（生产推荐）

生产环境建议通过环境变量注入敏感配置：

```bash
export OSS_ACCESS_KEY_ID="LTAI5t..."
export OSS_ACCESS_KEY_SECRET="xxxx..."
```

## 阿里云 OSS Bucket 配置建议

### 1. Bucket 权限
- 设置为**私有**，禁止公网直读
- 强制通过预签名 URL 访问

### 2. 生命周期规则
按前缀 `v*` 配置：
- 30 天后转为低频访问
- 365 天后转为归档存储
- 1095 天后自动删除

### 3. 访问日志
- 开启访问日志
- 投递至 SLS（日志服务）用于审计

### 4. CDN 加速（可选）
- 绑定自定义 CDN 域名
- 源站设置为 OSS 内网端点
- 适用于公开 Skill 的全球分发

## API 使用示例

### 1. 创建 Skill

```bash
curl -X POST http://localhost:8080/api/v1/tdagent/skills/cloud/users/user-123 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "data-analysis",
    "description": "数据分析 Skill",
    "skillMarkdown": "---\nname: data-analysis\ndescription: 数据分析工具\n---\n...",
    "resources": {
      "scripts/analyze.py": "#!/usr/bin/env python3\n...",
      "references/format.md": "# 数据格式说明\n..."
    },
    "tags": ["data", "analysis"],
    "version": "1.0.0",
    "publish": true
  }'
```

### 2. 更新 Skill（创建新版本）

```bash
curl -X PUT http://localhost:8080/api/v1/tdagent/skills/cloud/users/user-123/skill-id-456/versions \
  -H "Content-Type: application/json" \
  -d '{
    "skillMarkdown": "---\nname: data-analysis\ndescription: 数据分析工具 v2\n---\n...",
    "resources": {
      "scripts/analyze.py": "#!/usr/bin/env python3\n# 更新内容..."
    },
    "version": "2.0.0"
  }'
```

### 3. 发布 Skill

```bash
curl -X POST http://localhost:8080/api/v1/tdagent/skills/cloud/users/user-123/skill-id-456/publish
```

### 4. 获取下载 URL

```bash
curl http://localhost:8080/api/v1/tdagent/skills/cloud/users/user-123/skill-id-456/download
```

返回：
```json
{
  "downloadUrl": "https://agent-skills-prod.oss-cn-hangzhou.aliyuncs.com/..."
}
```

### 5. 分页查询

```bash
curl "http://localhost:8080/api/v1/tdagent/skills/cloud/page?userId=user-123&pageNum=1&pageSize=20&status=published"
```

## 存储目录结构

OSS 中的实际存储结构：

```
oss://agent-skills-prod/
└── agent-skills/                    # base-path
    └── {user_id}/                   # 用户隔离
        └── {skill_id}/              # Skill 隔离
            └── v1.0.0/              # 版本隔离
                └── skill.tar.gz     # 打包文件（包含 SKILL.md + resources）
```

## 版本回滚流程

1. 查询历史版本（通过 fileManifest 字段）
2. 重新发布指定版本：
   ```bash
   curl -X POST http://localhost:8080/api/v1/tdagent/skills/cloud/users/user-123/skill-id-456/publish
   ```
   （在请求中指定 targetVersion）

## 安全注意事项

1. **AccessKey 管理**
   - 生产环境必须使用 RAM 子账号
   - 禁止硬编码在代码中
   - 建议通过配置中心（如 Nacos、Apollo）或环境变量注入

2. **权限隔离**
   - DB 层按 `user_id` 做行级过滤
   - OSS 层通过 RAM Policy 限制 Bucket 前缀访问
   - 所有 API 端点必须通过认证

3. **脚本安全**
   - 存储 ≠ 执行
   - 脚本下载后必须经过沙箱扫描
   - 限制 CPU/内存/网络出口

4. **审计追溯**
   - 开启 OSS 访问日志
   - 记录 MySQL Binlog
   - 记录 `who-when-what` 全链路
