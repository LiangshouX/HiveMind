/**
 * 应用层 - 用例编排和应用服务。
 *
 * <p>该包负责编排领域对象之间的交互，实现具体的业务用例。</p>
 *
 * <h2>结构说明</h2>
 * <ul>
 *   <li><b>service 接口</b> - 定义应用服务契约（ITdAgentChatService、ITdAgentStreamingService 等）</li>
 *   <li><b>impl/</b> - 应用服务实现，编排领域对象完成具体用例</li>
 *   <li><b>dto/</b> - 数据传输对象，用于跨层传递数据（ChatRequest、ChatResponse 等）</li>
 * </ul>
 *
 * <h2>设计原则</h2>
 * <ul>
 *   <li>应用服务只包含编排逻辑，不包含业务规则</li>
 *   <li>业务规则应该封装在领域层的聚合根和服务中</li>
 *   <li>DTO 仅用于数据传输，不包含业务逻辑</li>
 * </ul>
 */
package com.liangshou.agentic.application;
