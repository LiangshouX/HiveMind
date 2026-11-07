package com.liangshou.agentic.application;

import com.liangshou.agentic.application.dto.CompactContextResponse;
import com.liangshou.agentic.application.dto.SessionContextUsageResponse;

/**
 * 会话上下文用量服务。
 *
 * <p>负责统计当前会话占用的上下文窗口比例、思考次数与工具调用次数，
 * 并提供手动触发上下文压缩的能力。</p>
 *
 * @author LiangshouX
 */
public interface IContextUsageService {

    /**
     * 统计指定会话的上下文占用情况。
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @return 上下文占用统计
     */
    SessionContextUsageResponse getUsage(String userId, String sessionId);

    /**
     * 手动触发上下文压缩。
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @return 压缩结果
     */
    CompactContextResponse compact(String userId, String sessionId);
}
