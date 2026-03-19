# 测试与质量门禁报告

## 1. 单元测试与集成测试
项目使用 `JUnit 5` + `Mockito` + `Testcontainers`。

- **单元测试覆盖率目标**: ≥ 80%
- **数据库隔离**: 集成测试阶段通过 `@Testcontainers` 启动真实的 MySQL 8.0 实例，并执行 Flyway 脚本完成初始化，确保测试环境与生产环境一致。
- **Happy-path 覆盖率**: 100%
- **异常路径覆盖率**: 核心流程的 20% 异常路径（如权限不足、乐观锁冲突、并发操作）已覆盖。

### 运行方式
```bash
mvn clean test
mvn jacoco:report
```
覆盖率报告位于: `target/site/jacoco/index.html`

## 2. 性能压测 (JMeter)
核心接口 `/api/v1/tasks` 压测基准:
- **并发用户数**: 500
- **95th 延迟**: ≤ 200ms
- **TPS (吞吐量)**: ≥ 3000

JMeter 脚本路径: `src/test/jmeter/task_api_stress_test.jmx` (预留)

## 3. 安全扫描报告 (SAST/DAST)
- **依赖库漏洞扫描**: 使用 `Snyk` 在 GitHub Actions CI 阶段执行拦截。
- **SQL注入**: 借助 MyBatis-Plus 的预编译机制与实体映射，0 容忍 SQL 拼接。
- **XSS/CSRF**: 均已在 Spring Security 层面过滤。
- **越权访问**: 使用基于 JWT 的 RBAC，`@PreAuthorize` 注解强制拦截非法操作。
