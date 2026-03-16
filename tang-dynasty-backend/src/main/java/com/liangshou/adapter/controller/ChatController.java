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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ChatModel chatModel;

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
        String userContent = "";
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            ChatMessage lastMsg = request.getMessages().get(request.getMessages().size() - 1);
            userContent = lastMsg.getContent();
            
            MessagePO userMsg = new MessagePO();
            userMsg.setSessionId(sessionId);
            userMsg.setRole(lastMsg.getRole());
            userMsg.setContent(userContent);
            userMsg.setCreateTime(LocalDateTime.now());
            messageService.save(userMsg);
        }

        // Build Prompt for AI
        // Retrieve history (optional, for now just use current message or last few)
        // Ideally we should load history from DB or use the request messages
        List<Message> promptMessages = new ArrayList<>();
        promptMessages.add(new SystemMessage("You are a helpful AI assistant in the Tang Dynasty system."));
        promptMessages.add(new UserMessage(userContent));
        
        Prompt prompt = new Prompt(promptMessages);
        ChatResponse chatResponse = chatModel.call(prompt);
        String aiContent = chatResponse.getResult().getOutput().getContent();

        // Save AI Response
        ChatMessage response = new ChatMessage();
        response.setRole("assistant");
        response.setContent(aiContent);
        
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
