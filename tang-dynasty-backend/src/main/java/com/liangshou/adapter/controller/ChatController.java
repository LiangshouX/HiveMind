package com.liangshou.adapter.controller;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.liangshou.common.Result;
import com.liangshou.infrastructure.datasource.po.ConversationPO;
import com.liangshou.infrastructure.datasource.po.MessagePO;
import com.liangshou.service.ConversationService;
import com.liangshou.service.MessageService;
import com.liangshou.service.dto.ChatMessage;
import com.liangshou.service.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    @PostMapping("/completions")
    public Result<ChatMessage> chat(@RequestBody ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = IdUtil.fastSimpleUUID();
            ConversationPO conversation = new ConversationPO();
            conversation.setSessionId(sessionId);
            conversation.setTitle("New Chat");
            conversation.setType("chat");
            conversation.setCreateTime(LocalDateTime.now());
            conversation.setUpdateTime(LocalDateTime.now());
            conversationService.save(conversation);
        }

        // Save User Message
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            ChatMessage lastMsg = request.getMessages().get(request.getMessages().size() - 1);
            MessagePO userMsg = new MessagePO();
            userMsg.setSessionId(sessionId);
            userMsg.setRole(lastMsg.getRole());
            userMsg.setContent(lastMsg.getContent());
            userMsg.setCreateTime(LocalDateTime.now());
            messageService.save(userMsg);
        }

        // Mock AI Response
        ChatMessage response = new ChatMessage();
        response.setRole("assistant");
        response.setContent("Received message. AI Agent module is not yet integrated.");
        
        MessagePO aiMsg = new MessagePO();
        aiMsg.setSessionId(sessionId);
        aiMsg.setRole(response.getRole());
        aiMsg.setContent(response.getContent());
        aiMsg.setCreateTime(LocalDateTime.now());
        messageService.save(aiMsg);

        return Result.success(response);
    }
    
    @GetMapping("/conversations")
    public Result<List<ConversationPO>> listConversations() {
        return Result.success(conversationService.list());
    }

    @GetMapping("/conversations/{sessionId}/messages")
    public Result<List<MessagePO>> listMessages(@PathVariable String sessionId) {
        return Result.success(messageService.list(new LambdaQueryWrapper<MessagePO>()
                .eq(MessagePO::getSessionId, sessionId)
                .orderByAsc(MessagePO::getCreateTime)));
    }
}
