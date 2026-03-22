# Tang Dynasty 后端项目 (tang-dynasty-backend)

本项目基于 Spring Boot 构建，是 Tang Dynasty 多 Agent 协作平台的后端核心服务。严格遵循了《Tang Dynasty 后端详设说明书》的架构和规范。

## 架构说明
本项目采用严格的单向依赖分层架构：
- `adapter/controller`: 适配层，接收 HTTP 请求，封装 DTO/VO，无业务逻辑。
- `service` & `service/impl`: 业务逻辑层，处理业务编排和事务。
- `infrastructure/datasource/support`: 数据支持层，封装单表 CRUD，对上层屏蔽 Mapper。
- `infrastructure/datasource/mapper`: 数据访问层，MyBatis-Plus 接口。
- `infrastructure/datasource/po`: 持久化对象层，与数据库表一一对应。
- `common/utils`: 通用工具层，如加密、JWT、统一响应等。

**依赖规则：Controller -> Service -> Support -> Mapper -> PO，禁止跨层或逆向调用。**

## 技术栈
- Spring Boot 3.2.x
- MyBatis-Plus 3.5.x
- Spring Security + JWT
- Springdoc OpenAPI 3 (Swagger)
- MySQL 8.0
- Hutool 5.8.x

## 本地启动步骤
1. **环境准备**: 确保已安装 JDK 17 和 Maven。
2. **数据库准备**: 在本地 MySQL (端口 3306) 中执行 `src/main/resources/db/init/init_v1_20260322.sql` 初始化数据库。
3. **环境变量**: 配置本地环境变量，如数据库密码等。
4. **编译构建**:
   ```bash
   mvn clean package
   ```
5. **启动项目**:
   在 `tang-dynasty-launcher` 模块下运行 `TangApplication.java`（本项目作为依赖被引入），或者如果本项目配置了启动类，则直接运行本项目的启动类。

## 服务访问
- **应用端口**: 默认 8080
- **Swagger 接口文档**: `http://localhost:8080/swagger-ui.html`

## 代码质量
所有代码均通过严格单测：
- Controller 层：MockMvc 测试
- Service 层：Mockito 业务打桩
- Support 层：MybatisPlusTest（或 Mock）单测
构建过程无任何编译警告或失败。
