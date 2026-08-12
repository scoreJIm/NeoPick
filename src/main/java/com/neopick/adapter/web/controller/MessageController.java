package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.adapter.web.dto.message.ConversationResponse;
import com.neopick.adapter.web.dto.message.MessageResponse;
import com.neopick.adapter.web.dto.message.SendMessageRequest;
import com.neopick.adapter.web.dto.message.StartConversationRequest;
import com.neopick.application.message.MessageUseCase;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class MessageController {

    private final MessageUseCase messageUseCase;

    public MessageController(MessageUseCase messageUseCase) {
        this.messageUseCase = messageUseCase;
    }

    @GetMapping("/conversations")
    @Timed(value = "neopick.messages.list_conversations", description = "List conversations")
    public ApiResponse<List<ConversationResponse>> listConversations() {
        var convs = messageUseCase.listConversations();
        return ApiResponse.success(convs.stream().map(ConversationResponse::from).toList());
    }

    @PostMapping("/conversations")
    @Timed(value = "neopick.messages.start_conversation", description = "Start conversation")
    public ApiResponse<ConversationResponse> startConversation(
            @Valid @RequestBody StartConversationRequest request) {
        var conv = messageUseCase.startConversation(request.teacherId());
        return ApiResponse.success(ConversationResponse.from(conv));
    }

    @GetMapping("/conversations/{id}/messages")
    @Timed(value = "neopick.messages.get_messages", description = "Get conversation messages")
    public ApiResponse<List<MessageResponse>> getMessages(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var messages = messageUseCase.getMessages(id, page, size);
        return ApiResponse.success(messages.stream().map(MessageResponse::from).toList());
    }

    @PostMapping("/conversations/{id}/messages")
    @Timed(value = "neopick.messages.send", description = "Send message")
    public ApiResponse<MessageResponse> sendMessage(
            @PathVariable String id,
            @Valid @RequestBody SendMessageRequest request) {
        var msg = messageUseCase.sendMessage(
                new MessageUseCase.SendMessageCommand(id, request.content()));
        return ApiResponse.success(MessageResponse.from(msg));
    }
}
