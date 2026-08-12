package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.adapter.web.dto.message.ConversationResponse;
import com.neopick.adapter.web.dto.message.MessageResponse;
import com.neopick.adapter.web.dto.message.SendMessageRequest;
import com.neopick.adapter.web.dto.message.StartConversationRequest;
import com.neopick.application.message.MessageUseCase;
import com.neopick.infrastructure.ratelimit.RateLimit;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Messages", description = "Conversation and messaging between students and teachers")
@SecurityRequirement(name = "bearerAuth")
public class MessageController {

    private final MessageUseCase messageUseCase;

    public MessageController(MessageUseCase messageUseCase) {
        this.messageUseCase = messageUseCase;
    }

    @GetMapping("/conversations")
    @RateLimit(limit = 60, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.messages.list_conversations", description = "List conversations")
    @Operation(summary = "List conversations", description = "Returns all conversations for the authenticated user, ordered by most recent message.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversations returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ApiResponse<List<ConversationResponse>> listConversations() {
        var convs = messageUseCase.listConversations();
        return ApiResponse.success(convs.stream().map(ConversationResponse::from).toList());
    }

    @PostMapping("/conversations")
    @Timed(value = "neopick.messages.start_conversation", description = "Start conversation")
    @Operation(summary = "Start a conversation", description = "Creates a new conversation between the authenticated student and a teacher. Returns the conversation details.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversation created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid teacher ID", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Teacher not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conversation already exists with this teacher", content = @Content)
    })
    public ApiResponse<ConversationResponse> startConversation(
            @Valid @RequestBody StartConversationRequest request) {
        var conv = messageUseCase.startConversation(request.teacherId());
        return ApiResponse.success(ConversationResponse.from(conv));
    }

    @GetMapping("/conversations/{id}/messages")
    @RateLimit(limit = 60, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.messages.get_messages", description = "Get conversation messages")
    @Operation(summary = "Get conversation messages", description = "Returns messages for a specific conversation, paginated with newest last.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Messages returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversation not found", content = @Content)
    })
    public ApiResponse<List<MessageResponse>> getMessages(
            @Parameter(description = "Conversation ID (UUID format)") @PathVariable String id,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size) {
        var messages = messageUseCase.getMessages(id, page, size);
        return ApiResponse.success(messages.stream().map(MessageResponse::from).toList());
    }

    @PostMapping("/conversations/{id}/messages")
    @RateLimit(limit = 30, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.messages.send", description = "Send message")
    @Operation(summary = "Send a message", description = "Sends a message in an existing conversation. The message is delivered in real-time via WebSocket if the recipient is online.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Message sent successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Message content is empty", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Conversation not found", content = @Content)
    })
    public ApiResponse<MessageResponse> sendMessage(
            @Parameter(description = "Conversation ID (UUID format)") @PathVariable String id,
            @Valid @RequestBody SendMessageRequest request) {
        var msg = messageUseCase.sendMessage(
                new MessageUseCase.SendMessageCommand(id, request.content()));
        return ApiResponse.success(MessageResponse.from(msg));
    }
}
