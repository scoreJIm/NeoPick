package com.neopick.application.message;

import com.neopick.domain.message.*;
import com.neopick.port.security.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MessageUseCase {

    private final ConversationRepository conversationRepository;
    private final SecurityContext securityContext;

    public MessageUseCase(ConversationRepository conversationRepository,
                          SecurityContext securityContext) {
        this.conversationRepository = conversationRepository;
        this.securityContext = securityContext;
    }

    public List<Conversation> listConversations() {
        String userId = securityContext.requireCurrentUserId();
        return conversationRepository.findByStudentId(userId);
    }

    @Transactional
    public Conversation startConversation(Long teacherId) {
        String studentId = securityContext.requireCurrentUserId();
        return conversationRepository.findByStudentAndTeacher(studentId, teacherId)
                .orElseGet(() -> conversationRepository.save(
                        new Conversation(ConversationId.generate(), studentId, teacherId)));
    }

    public List<ChatMessage> getMessages(String conversationId, int page, int size) {
        return conversationRepository.findMessages(conversationId, page, size);
    }

    @Transactional
    public ChatMessage sendMessage(SendMessageCommand command) {
        String senderId = securityContext.requireCurrentUserId();
        return sendMessage(senderId, command);
    }

    @Transactional
    public ChatMessage sendMessage(String senderId, SendMessageCommand command) {
        Conversation conv = conversationRepository.findById(
                        ConversationId.from(command.conversationId()))
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        String receiverId = conv.getStudentId().equals(senderId)
                ? conv.getTeacherId().toString() : conv.getStudentId();
        ChatMessage message = new ChatMessage(UUID.randomUUID(), command.conversationId(),
                senderId, receiverId, command.content(), MessageType.TEXT);
        conv.updateLastMessage(command.content(), message.getSentAt());
        conversationRepository.save(conv);
        return conversationRepository.saveMessage(message);
    }

    @Transactional
    public void markMessagesAsRead(String conversationId, String readerId) {
        conversationRepository.findById(ConversationId.from(conversationId))
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
        conversationRepository.markMessagesAsRead(conversationId, readerId);
    }

    public record SendMessageCommand(String conversationId, String content) {}
}
