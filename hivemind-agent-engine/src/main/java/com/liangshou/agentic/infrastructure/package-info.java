/**
 * 基础设施层 - 技术实现和外部系统集成。
 *
 * <p>此包包含所有技术实现细节,包括数据库访问和外部服务集成。</p>
 *
 * <h2>子包说明</h2>
 * <ul>
 *   <li><b>mongo/</b> - MongoDB 数据访问实现
 *     <ul>
 *       <li>repository/ - Spring Data MongoDB Repository 实现</li>
 *       <li>config/ - MongoDB 诊断配置</li>
 *     </ul>
 *   </li>
 *   <li><b>mysql/</b> - MySQL 数据访问实现
 *     <ul>
 *       <li>mapper/ - MyBatis Plus Mapper 接口</li>
 *       <li>po/ - 持久化对象(Persistent Objects)</li>
 *       <li>repository/ - 基于 Mapper 的 Repository 实现</li>
 *     </ul>
 *   </li>
 *   <li><b>sandbox/</b> - 沙箱环境管理</li>
 *   <li><b>config/</b> - Spring 配置类(TdAgentProperties 等)</li>
 * </ul>
 *
 * <h2>设计原则</h2>
 * <ul>
 *   <li>基础设施层实现领域层定义的 repository 接口</li>
 *   <li>领域模型(Documents)已移至领域子包</li>
 *   <li>基础设施层可以依赖 Spring、MongoDB、MyBatis 等技术框架</li>
 * </ul>
 */
package com.liangshou.agentic.infrastructure;
