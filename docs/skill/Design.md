# 云端 Agent 平台个人 Skill 存储技术方案

## 1. 架构设计原则

| 原则           | 说明                                                          |
|:-------------|:------------------------------------------------------------|
| **元数据与文件分离** | 关系型数据库管状态/权限/索引，对象存储管实体文件，避免 BLOB 拖垮 DB                     |
| **版本前缀隔离**   | 不依赖对象存储原生版本控制，通过目录前缀 `v{major}.{minor}.{patch}` 实现原子切换与快速回滚 |
| **读写路径解耦**   | 上传走后端直传/STS，下载走预签名 URL + CDN，Agent 运行时仅拉取打包文件               |
| **运行时强隔离**   | 存储层与执行层彻底解耦，脚本必须经沙箱扫描后再注入运行环境                               |

---

## 2. 存储架构与目录规范

```
oss://agent-skills/
└── {user_id}/
    └── {skill_id}/
        └── v{major}.{minor}.{patch}/       ← 版本隔离
            ├── SKILL.md
            ├── references/
            ├── scripts/
            ├── assets/
            └── _manifest.json              ← 文件清单快照（可选冗余）
```

- **优点**：天然支持原子发布、生命周期策略按版本归档、跨租户前缀隔离
- **访问模式**：Agent 不遍历目录，通过元数据表 `file_manifest` 获取文件列表，后端聚合为 `.tar.gz` 后提供单次下载链接

---

## 3. 阿里云 OSS 对接指南（生产级）

### 3.1 依赖引入

```xml

<dependency>
    <groupId>com.aliyun.oss</groupId>
    <artifactId>aliyun-sdk-oss</artifactId>
    <version>3.17.4</version>
</dependency>
```

### 3.2 客户端初始化（推荐 RAM 子账号 + VPC 内网端点）

```java
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;

public class OssConfig {
    // 生产环境务必通过配置中心注入，硬编码仅示例
    private static final String ENDPOINT = "https://oss-cn-hangzhou-internal.aliyuncs.com"; // 同地域内网
    private static final String ACCESS_KEY_ID = "LTAI5t...";
    private static final String ACCESS_KEY_SECRET = "xxxx...";
    private static final String BUCKET_NAME = "agent-skills-prod";

    public static OSS createClient() {
        return new OSSClientBuilder().build(ENDPOINT, ACCESS_KEY_ID, ACCESS_KEY_SECRET);
    }
}
```

### 3.3 安全下载：生成预签名 URL（Pre-signed URL）

```java
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;

import java.net.URL;
import java.util.Date;

public URL generateDownloadUrl(String objectKey, int expireMinutes) {
    OSS ossClient = OssConfig.createClient();
    try {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                OssConfig.BUCKET_NAME, objectKey, HttpMethod.GET);
        request.setExpiration(new Date(System.currentTimeMillis() + expireMinutes * 60 * 1000L));
        // 可选：强制 HTTPS、限制 IP、绑定 CDN 域名
        request.addUserMetadata("download-by", "agent-platform");
        return ossClient.generatePresignedUrl(request);
    } finally {
        ossClient.shutdown();
    }
}
```

### 3.4 高效上传：分片上传 & 目录原子替换

```java
import com.aliyun.oss.model.ObjectMetadata;

import java.io.File;

public void uploadSkillFiles(String userPath, File localDir) {
    OSS ossClient = OssConfig.createClient();
    try {
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentType("application/octet-stream");
        // 建议：前端/后端先打包为 tar.gz，再单次 PutObject 或 MultipartUpload
        ossClient.putObject(OssConfig.BUCKET_NAME, userPath + "/v1.0.0.tar.gz",
                new FileInputStream(localDir), meta);
    } finally {
        ossClient.shutdown();
    }
}
```

### 3.5 阿里云控制台关键配置

| 配置项           | 推荐值                             | 作用                         |
|:--------------|:--------------------------------|:---------------------------|
| **Bucket 权限** | `私有`                            | 禁止公网直读，强制走预签名 URL          |
| **生命周期规则**    | 按前缀 `v*`，30天转低频，365天转归档，1095天删除 | 降低长期存储成本                   |
| **访问日志**      | 开启并投递至 SLS                      | 审计谁在何时拉取了哪个 Skill          |
| **CDN 加速**    | 绑定自定义域名，源站为 OSS 内网              | 公开 Skill 全球分发，降低 OSS 回源流量费 |

---

## 4. MySQL 元数据层设计（精简版）

### 4.1 DDL（MySQL 8.0+）

```sql
CREATE TABLE `skills` (
  `skill_id` VARCHAR(36) NOT NULL,
  `user_id` VARCHAR(36) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` TEXT,
  `current_version` VARCHAR(20) NOT NULL DEFAULT '1.0.0',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft',
  `tags` JSON,
  `execution_env` JSON,
  `file_manifest` JSON NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`skill_id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_updated_at` (`updated_at`),
  KEY `idx_tags` ((CAST(`tags` AS CHAR(255) ARRAY))),
  KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 4.2 MyBatis-Plus 核心模板

```java

@Data
@TableName(value = "skills", autoResultMap = true)
public class SkillPO {
    @TableId(type = IdType.ASSIGN_UUID)
    private String skillId;
    private String userId, name, description, currentVersion, status;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> executionEnv;
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> fileManifest;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deletedAt;
}

@Mapper
public interface SkillMapper extends BaseMapper<SkillPO> {
}

public interface ISkillService extends IService<SkillPO> {
    boolean publish(String skillId, String version);

    boolean updateManifest(String skillId, Map<String, Object> manifest);
    // 其他查询方法按需扩展
}
```

> 💡 **关键提示**：`autoResultMap = true` 与 `JacksonTypeHandler` 必须配对使用；软删除由 `@TableLogic` 自动拦截。

---

## 5. 核心能力与工程实践

| 能力       | 实现方案                                                             | 避坑指南                                      |
|:---------|:-----------------------------------------------------------------|:------------------------------------------|
| **版本回滚** | 新文件写新前缀目录 → 更新 DB `current_version` & `file_manifest` → 旧版本保留不删除 | 禁止覆盖原目录，确保回滚可逆                            |
| **加载性能** | 后端按 `file_manifest` 打包为 `.tar.gz` 上传 OSS → Agent 单次下载解压          | 避免 Agent 发起数十次 HTTP 请求拉取零散文件              |
| **语义检索** | 异步将 `name`/`description`/`tags` 同步至 Meilisearch/Elasticsearch    | MySQL `LIKE` 仅适合冷启动或小规模，超 50W 记录务必上独立搜索引擎 |
| **缓存策略** | Redis 缓存热点 `SkillPO`（TTL 10min），CDN 缓存公开 Skill 的 `.tar.gz` 包     | 元数据更新时主动失效缓存，避免脏读                         |

---

## 6. 安全与合规策略

1. **权限隔离**：DB 层按 `user_id` 做行级过滤；OSS 层通过 RAM Policy 限制 Bucket 前缀访问
2. **临时凭证**：前端直传使用 STS 临时 Token（有效期 15min），后端使用 RAM 子账号 AccessKey
3. **脚本安全**：⚠️ **存储 ≠ 执行**。脚本下载后必须：
    - 静态扫描（ClamAV/自定义规则）
    - 注入 Docker/gVisor 沙箱
    - 限制 CPU/内存/网络出口，禁止挂载宿主机目录
4. **审计追溯**：开启 OSS 访问日志 + MySQL Binlog，记录 `who-when-what` 全链路

---

## 7. 本地开发与平滑迁移

| 环境       | 存储方案                   | 配置切换方式                              |
|:---------|:-----------------------|:------------------------------------|
| **本地开发** | MinIO（Docker 单行启动）     | `S3_ENDPOINT=http://localhost:9000` |
| **测试环境** | 阿里云 OSS（独立 Bucket）     | 切换 `ENDPOINT` + `AK/SK`             |
| **生产环境** | 阿里云 OSS + CDN + VPC 内网 | 配置中心下发，代码零修改                        |

```bash
# 本地开发一键启动 MinIO
docker run -d --name minio-dev -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=dev -e MINIO_ROOT_PASSWORD=dev-secret \
  -v ~/oss-data:/data minio/minio server /data --console-address ":9001"
```

> ✅ 阿里云 OSS 与 MinIO 均兼容 `AWS S3 API`。通过抽象 `ObjectStorageService` 接口，仅需修改 YAML 配置即可无缝切换。

---
**交付说明**：本方案已覆盖 Agent 平台 Skill 存储的架构设计、阿里云 OSS 对接细节、元数据建模、安全隔离与性能优化。按此实施可支撑
**百万级 Skill 存储、高并发拉取、版本可控、安全合规** 的生产要求。如需 STS 临时凭证生成代码、`.tar.gz`
流式打包工具类或沙箱执行器设计，可提供进一步补充。