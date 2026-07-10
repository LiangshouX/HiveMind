package com.liangshou.agentic.adapter.controller;

import com.liangshou.agentic.agents.guard.approval.ToolApprovalService;
import com.liangshou.agentic.agents.provider.TdAgentModelDescriptor;
import com.liangshou.agentic.agents.provider.TdAgentProviderDescriptor;
import com.liangshou.agentic.agents.provider.TdAgentProviderRegistry;
import com.liangshou.agentic.application.ITdAgentStreamingService;
import com.liangshou.agentic.domain.memory.model.ConversationViewDocument;
import com.liangshou.agentic.application.ITdAgentChatService;
import com.liangshou.agentic.application.dto.*;
import com.liangshou.agentic.common.utils.Result;
import com.liangshou.agentic.infrastructure.provider.DbProviderConfigLoader;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent 聊天 REST API 控制器，提供同步和流式对话接口。
 *
 * @author LiangshouX
 */
@RestController
@RequestMapping("/api/v1/tdagent")
@SuppressWarnings("unused")
public class TdAgentChatController {

    private static final Logger log = LoggerFactory.getLogger(TdAgentChatController.class);

    private final ITdAgentChatService chatService;
    private final ITdAgentStreamingService streamingService;
    private final ToolApprovalService toolApprovalService;
    private final JdbcTemplate jdbcTemplate;
    private final TdAgentProviderRegistry providerRegistry;
    private final DbProviderConfigLoader dbConfigLoader;

    public TdAgentChatController(
            ITdAgentChatService chatService,
            ITdAgentStreamingService streamingService,
            ToolApprovalService toolApprovalService,
            JdbcTemplate jdbcTemplate,
            TdAgentProviderRegistry providerRegistry,
            DbProviderConfigLoader dbConfigLoader) {
        this.chatService = chatService;
        this.streamingService = streamingService;
        this.toolApprovalService = toolApprovalService;
        this.jdbcTemplate = jdbcTemplate;
        this.providerRegistry = providerRegistry;
        this.dbConfigLoader = dbConfigLoader;
    }

    @PostMapping("/chat")
    public ChatResponse chat(Principal principal, @Valid @RequestBody ChatRequest request) {
        applyCurrentUser(principal, request);
        return chatService.chat(request);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Principal principal, @Valid @RequestBody ChatRequest request) {
        log.info("[流式聊天] 收到流式请求 - userId: {}, sessionId: {}, message: {}",
                principal != null ? principal.getName() : "unknown",
                request.getSessionId(),
                request.getMessage());
        applyCurrentUser(principal, request);
        SseEmitter emitter = streamingService.stream(request);
        log.info("[流式聊天] SSE Emitter 已创建并返回 - userId: {}, sessionId: {}",
                request.getUserId(), request.getSessionId());
        return emitter;
    }

    @PostMapping(value = "/approvals/approve", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter approve(Principal principal, @Valid @RequestBody ToolApprovalActionRequest request) {
        log.info("[工具审批] 收到批准请求 - userId: {}, sessionId: {}, approvalIds: {}",
                principal != null ? principal.getName() : "unknown",
                request.getSessionId(),
                request.getApprovalIds());
        request.setUserId(currentUserId(principal));
        return streamingService.approveAndResume(request);
    }

    @PostMapping(value = "/approvals/reject", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter reject(Principal principal, @Valid @RequestBody ToolApprovalActionRequest request) {
        log.info("[工具审批] 收到拒绝请求 - userId: {}, sessionId: {}, approvalIds: {}",
                principal != null ? principal.getName() : "unknown",
                request.getSessionId(),
                request.getApprovalIds());
        request.setUserId(currentUserId(principal));
        return streamingService.rejectAndResume(request);
    }

    @GetMapping("/approvals/me/{sessionId}")
    public List<?> listApprovals(Principal principal, @PathVariable String sessionId) {
        return toolApprovalService.listPending(currentUserId(principal), sessionId);
    }

    @PostMapping("/chat/interrupt")
    public ChatResponse interrupt(Principal principal, @Valid @RequestBody InterruptRequest request) {
        String userId = currentUserId(principal);
        log.info("[中断会话] 收到中断请求 - userId: {}, sessionId: {}", userId, request.getSessionId());
        request.setUserId(userId);
        boolean interrupted = streamingService.interrupt(userId, request.getSessionId());
        log.info("[中断会话] 中断结果 - userId: {}, sessionId: {}, success: {}",
                userId, request.getSessionId(), interrupted);
        return ChatResponse.builder()
                .success(interrupted)
                .commandHandled(false)
                .userId(userId)
                .sessionId(request.getSessionId())
                .message(interrupted ? "已发送中断信号。" : "当前没有活动中的流式会话。")
                .messageCount(0)
                .timestamp(java.time.Instant.now().toString())
                .metadata(java.util.Map.of("interrupted", interrupted))
                .build();
    }

    @GetMapping("/sessions/me")
    public List<ConversationViewDocument> listSessions(Principal principal) {
        return chatService.listSessions(currentUserId(principal));
    }

    @GetMapping("/sessions/me/{sessionId}")
    public SessionHistoryResponse getSessionHistory(Principal principal, @PathVariable String sessionId) {
        return chatService.getSessionHistory(currentUserId(principal), sessionId);
    }

    @DeleteMapping("/sessions/me/{sessionId}")
    public void deleteSession(Principal principal, @PathVariable String sessionId) {
        chatService.deleteSession(currentUserId(principal), sessionId);
    }

    /**
     * 获取当前用户已激活的模型列表。
     */
    @GetMapping("/active-models")
    public Result<ActiveModelsResponse> getActiveModels(Principal principal) {
        String userId = currentUserId(principal);

        // 使用 JdbcTemplate 查询用户已激活的供应商
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT DISTINCT model_provider_id, provider_name, model_provider_type " +
                "FROM sys_models WHERE user_id = ? AND is_provider_activated = 1",
                userId);

        // 获取内置供应商目录
        List<TdAgentProviderDescriptor> catalogProviders = providerRegistry.listProviders();
        Map<String, TdAgentProviderDescriptor> catalogMap = catalogProviders.stream()
                .collect(Collectors.toMap(TdAgentProviderDescriptor::getId, p -> p));

        // 从数据库加载用户自定义的供应商配置
        List<TdAgentProviderDescriptor> userProviders = dbConfigLoader.loadForUser(userId);
        Map<String, TdAgentProviderDescriptor> userProviderMap = userProviders.stream()
                .collect(Collectors.toMap(TdAgentProviderDescriptor::getId, p -> p));

        // 构建响应
        List<ActiveModelsResponse.ActiveProvider> providers = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String providerId = (String) row.get("model_provider_id");
            String providerName = (String) row.get("provider_name");
            String providerType = (String) row.get("model_provider_type");

            // 优先从用户自定义配置中获取，否则从内置目录获取
            TdAgentProviderDescriptor provider = userProviderMap.get(providerId);
            if (provider == null) {
                provider = catalogMap.get(providerId);
            }

            List<ActiveModelsResponse.ActiveModel> models = new ArrayList<>();
            if (provider != null && provider.getModels() != null) {
                for (TdAgentModelDescriptor model : provider.getModels()) {
                    models.add(ActiveModelsResponse.ActiveModel.builder()
                            .modelId(model.getId())
                            .modelName(model.getName())
                            .supportsMultimodal(model.isSupportsMultimodal())
                            .supportsVideo(model.isSupportsVideo())
                            .build());
                }
            }

            providers.add(ActiveModelsResponse.ActiveProvider.builder()
                    .providerId(providerId)
                    .providerName(providerName)
                    .providerType(providerType)
                    .models(models)
                    .build());
        }

        return Result.success(ActiveModelsResponse.builder().providers(providers).build());
    }

    private void applyCurrentUser(Principal principal, ChatRequest request) {
        request.setUserId(currentUserId(principal));
        if (request.getUserName() == null || request.getUserName().isBlank()) {
            request.setUserName(principal.getName());
        }
    }

    private String currentUserId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new IllegalArgumentException("未获取到当前登录用户");
        }
        return principal.getName();
    }
}
