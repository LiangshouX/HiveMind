/**
 * HiveMind Agent Engine - 基于 AgentScope-Java 的 AI Agent 后端引擎。
 *
 * <h2>架构概述</h2>
 * <p>本项目采用 <b>DDD（领域驱动设计）分层架构</b>，核心层包括：</p>
 *
 * <ul>
 *   <li><b>domain/</b> - 领域层：核心业务模型、枚举、工厂和服务接口</li>
 *   <li><b>application/</b> - 应用层：用例编排、应用服务和 DTO</li>
 *   <li><b>infrastructure/</b> - 基础设施层：技术实现（MongoDB、MySQL、沙箱等）</li>
 *   <li><b>adapter/</b> - 适配器层：对外接口（REST Controller）</li>
 *   <li><b>common/</b> - 通用层：跨领域共享的异常、工具类</li>
 * </ul>
 *
 * <h2>领域层结构</h2>
 * <pre>
 * domain/
 * ├── agent/          - Agent 核心（工厂、Hook、Prompt 服务）
 * ├── memory/         - 记忆管理（模型、服务、仓储接口）
 * ├── session/        - 会话管理（模型、服务、仓储接口）
 * ├── tool/           - 工具管理（Tool Guard、内置工具）
 * ├── skill/          - 技能管理（模型、服务、仓储接口）
 * ├── provider/       - 模型供应商（ProviderRegistry）
 * ├── streaming/      - 流式事件（事件类型、会话注册）
 * └── shared/         - 共享领域对象（枚举、常量）
 * </pre>
 *
 * <h2>技术栈</h2>
 * <ul>
 *   <li>AgentScope-Java 1.0.12 - AI Agent 框架</li>
 *   <li>Spring Boot 4.0 - Web 框架</li>
 *   <li>MongoDB - 对话历史和会话状态存储</li>
 *   <li>MySQL + MyBatis Plus - 元数据管理</li>
 *   <li>ReMe - 长期记忆服务</li>
 * </ul>
 *
 * @author LiangshouX
 * @version 1.0-SNAPSHOT
 */
package com.liangshou.agentic;
