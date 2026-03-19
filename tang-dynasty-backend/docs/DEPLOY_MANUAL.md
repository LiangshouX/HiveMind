# 上线手册与回滚方案

## 1. 部署环境要求
- JDK 17+
- MySQL 8.0+
- Docker 24.0+
- Kubernetes (可选，提供 Helm/Nacos 配置)

## 2. CI/CD 流程
通过 GitHub Actions 自动触发：
1. 代码检出 -> 2. Maven 编译 -> 3. Snyk 安全扫描 -> 4. Testcontainers 集成测试 -> 5. Docker 镜像构建并推送到 Harbor/DockerHub。

## 3. 手动部署指南

### 3.1 数据库初始化
服务启动时会自动运行 `Flyway` 脚本进行数据库迁移：
`src/main/resources/db/migration/V1.0.0__init_schema.sql`

若需要**回滚**：
手动执行 `U1.0.0__init_schema.sql`，或使用 Flyway Maven Plugin 的 undo 命令（需要 Flyway Pro 许可证，开源版可手动执行）。

### 3.2 启动服务 (Docker)
```bash
docker run -d \
  --name tang-dynasty-backend \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://<host>:3306/tang_dynasty" \
  -e SPRING_DATASOURCE_USERNAME="root" \
  -e SPRING_DATASOURCE_PASSWORD="password" \
  -e JWT_SECRET="your_secure_random_key_here" \
  -e AI_DASHSCOPE_API_KEY="sk-xxxxxx" \
  tang-dynasty-backend:latest
```

### 3.3 验证健康状态
访问Swagger文档验证：
http://localhost:8080/swagger-ui.html
或
http://localhost:8080/v3/api-docs

## 4. 回滚方案
1. **代码回滚**: 将镜像 Tag 回滚至上一个稳定版本。
2. **配置回滚**: 若在 Nacos 中，下发历史版本的配置。
3. **数据库回滚**: 除非必要，不要随意回退数据库。如涉及重大结构变更失败，运行对应的 `U<版本号>__xxx.sql` 进行逆向变更，并恢复数据快照。
