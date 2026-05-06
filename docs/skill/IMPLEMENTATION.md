# 云端 Skill 存储功能实施总结

## 概述

本次实施为基于 AgentScope 开发的 AI Agent 平台添加了云端个人 Skill 存储功能，采用：
- **MySQL** 存储 Skill 元信息（名称、描述、版本、标签、文件清单等）
- **阿里云 OSS** 存储 Skill 文件（SKILL.md、references、scripts 等）

## 实施内容

### 1. 依赖添加

在 `tang-dynasty-agent-engine/pom.xml` 中添加了：
- `aliyun-sdk-oss:3.17.4` - 阿里云 OSS SDK
- `commons-compress:1.26.1` - 用于 tar.gz 打包

### 2. 核心代码文件

#### 存储层（Infrastructure）

| 文件 | 说明 |
|------|------|
| `ObjectStorageService.java` | 对象存储服务接口（抽象层，支持多种后端） |
| `OssProperties.java` | OSS 配置属性类（支持 Spring Boot 配置注入） |
| `AliyunOssStorageService.java` | 阿里云 OSS 实现（上传/下载/删除/预签名URL） |
| `SkillFileStorageService.java` | Skill 文件管理服务（tar.gz 打包/版本管理） |

#### 数据层（MySQL）

| 文件 | 说明 |
|------|------|
| `SkillMetaManageSupport.java` | 扩展接口（增加版本管理方法） |
| `SkillMetaManageSupportImpl.java` | 扩展实现（增加创建/更新版本等方法） |
| `SkillMetaManagePO.java` | 已存在，Skill 元数据实体 |

#### DTO 层

| 文件 | 说明 |
|------|------|
| `SkillCreateRequest.java` | Skill 创建请求 DTO |
| `SkillVersionRequest.java` | Skill 版本更新请求 DTO |
| `SkillResponse.java` | Skill 响应 DTO |
| `SkillVersionInfo.java` | Skill 版本信息 DTO |
| `SkillPageQuery.java` | 已存在，分页查询 DTO |

#### 应用层

| 文件 | 说明 |
|------|------|
| `SkillApplicationService.java` | 应用层服务（编排元数据+文件存储） |

#### 控制器层

| 文件 | 说明 |
|------|------|
| `TdAgentSkillController.java` | 扩展控制器（增加云端 Skill API 端点） |

### 3. 文档和配置

| 文件 | 说明 |
|------|------|
| `docs/skill/CONFIG.md` | 配置示例和 API 使用文档 |
| `docs/skill/schema.sql` | MySQL 建表 SQL |
| `docs/skill/Design.md` | 已存在，设计文档 |

### 4. 测试

| 文件 | 说明 |
|------|------|
| `SkillFileStorageServiceTest.java` | 文件存储服务单元测试 |
| `SkillApplicationServiceIntegrationTest.java` | 应用服务集成测试 |

## API 端点

### 云端 Skill 管理 API（新增）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/tdagent/skills/cloud/users/{userId}` | 创建 Skill |
| PUT | `/api/v1/tdagent/skills/cloud/users/{userId}/{skillId}/versions` | 更新 Skill（新版本） |
| POST | `/api/v1/tdagent/skills/cloud/users/{userId}/{skillId}/publish` | 发布 Skill |
| POST | `/api/v1/tdagent/skills/cloud/users/{userId}/{skillId}/archive` | 归档 Skill |
| DELETE | `/api/v1/tdagent/skills/cloud/users/{userId}/{skillId}` | 删除 Skill |
| GET | `/api/v1/tdagent/skills/cloud/users/{userId}/{skillId}` | 获取 Skill 详情 |
| GET | `/api/v1/tdagent/skills/cloud/users/{userId}/{skillId}/download` | 获取下载 URL |
| GET | `/api/v1/tdagent/skills/cloud/page` | 分页查询 |

### 原有 API（保持不变）

原有基于 MongoDB 的 Skill 管理 API 继续可用，两者可以并存。

## 架构设计特点

### 1. 元数据与文件分离
- MySQL 管状态/权限/索引
- OSS 管实体文件
- 避免 BLOB 拖垮数据库

### 2. 版本前缀隔离
- 目录格式：`{userId}/{skillId}/v{major}.{minor}.{patch}/skill.tar.gz`
- 不依赖 OSS 原生版本控制
- 支持原子切换与快速回滚

### 3. 读写路径解耦
- 上传：后端直传 OSS
- 下载：预签名 URL + 可选 CDN
- Agent 运行时仅拉取打包文件

### 4. 条件启用
- 通过 `@ConditionalOnBean` 和 `@ConditionalOnProperty` 控制
- 未配置 OSS 时使用原有 MongoDB 存储
- 配置 OSS 后自动启用云端存储

## 配置方式

### 启用云端存储

在 `application.yml` 中添加：

```yaml
td:
  skill:
    storage:
      oss:
        enabled: true
        endpoint: https://oss-cn-hangzhou-internal.aliyuncs.com
        access-key-id: ${OSS_ACCESS_KEY_ID}
        access-key-secret: ${OSS_ACCESS_KEY_SECRET}
        bucket-name: agent-skills-prod
        base-path: agent-skills
        default-expire-minutes: 30
```

### 本地开发（MinIO）

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
```

启动 MinIO：
```bash
docker run -d --name minio-dev -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=dev -e MINIO_ROOT_PASSWORD=dev-secret \
  -v ~/oss-data:/data minio/minio server /data --console-address ":9001"
```

## 存储目录结构

```
oss://agent-skills-prod/
└── agent-skills/                    # base-path
    └── {user_id}/                   # 用户隔离
        └── {skill_id}/              # Skill 隔离
            └── v1.0.0/              # 版本隔离
                └── skill.tar.gz     # 打包文件
            └── v2.0.0/
                └── skill.tar.gz
```

## 数据库表

需要执行 `docs/skill/schema.sql` 创建 `skills_meta_manage` 表：

```sql
CREATE TABLE `skills_meta_manage` (
  `skill_id` VARCHAR(36) NOT NULL,
  `user_id` VARCHAR(36) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` TEXT,
  `current_version` VARCHAR(20) NOT NULL DEFAULT '1.0.0',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft',
  `tags` JSON,
  `dependencies` JSON,
  `execution_env` JSON,
  `file_manifest` JSON NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`skill_id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_user_name` (`user_id`, `name`),
  KEY `idx_updated_at` (`updated_at`),
  KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 编译和测试

### 编译

```bash
cd tang-dynasty-agent-engine
mvn clean compile -DskipTests
```

### 运行测试

```bash
mvn test
```

**注意**：首次编译需要下载 Maven 依赖，可能需要较长时间。

## 后续工作建议

1. **STS 临时凭证**：实现前端直传功能
2. **CDN 集成**：配置 CDN 加速公开 Skill 下载
3. **脚本安全沙箱**：实现脚本扫描和执行隔离
4. **搜索引擎集成**：将 Skill 元数据同步至 Meilisearch/Elasticsearch
5. **Redis 缓存**：缓存热点 Skill 元数据
6. **审计日志**：记录 OSS 访问和元数据变更日志

## 安全注意事项

1. **AccessKey 管理**：生产环境必须使用 RAM 子账号，禁止硬编码
2. **权限隔离**：DB 层按 `user_id` 做行级过滤
3. **脚本安全**：脚本下载后必须经沙箱扫描后再注入运行环境
4. **审计追溯**：开启 OSS 访问日志 + MySQL Binlog
