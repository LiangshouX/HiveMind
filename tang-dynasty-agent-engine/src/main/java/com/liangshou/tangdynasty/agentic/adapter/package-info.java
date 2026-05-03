/**
 * 适配器层 - 对外接口和 REST API。
 *
 * <p>该包包含所有对外暴露的接口，包括 REST Controller。</p>
 *
 * <h2>子包说明</h2>
 * <ul>
 *   <li><b>controller/</b> - REST 控制器，处理 HTTP 请求并返回响应</li>
 * </ul>
 *
 * <h2>API 端点</h2>
 * <ul>
 *   <li>POST /api/v1/tdagent/chat - 同步聊天</li>
 *   <li>POST /api/v1/tdagent/chat/stream - 流式聊天（SSE）</li>
 *   <li>POST /api/v1/tdagent/approvals/approve - 批准工具调用</li>
 *   <li>POST /api/v1/tdagent/approvals/reject - 拒绝工具调用</li>
 *   <li>POST /api/v1/tdagent/chat/interrupt - 中断流式会话</li>
 *   <li>GET /api/v1/tdagent/sessions/me - 列出会话</li>
 *   <li>GET /api/v1/tdagent/sessions/me/{sessionId} - 获取会话历史</li>
 *   <li>DELETE /api/v1/tdagent/sessions/me/{sessionId} - 删除会话</li>
 * </ul>
 */
package com.liangshou.tangdynasty.agentic.adapter;
