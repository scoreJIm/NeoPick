package com.neopick.domain.message;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository {

    Conversation save(Conversation conversation);

    Optional<Conversation> findById(ConversationId id);

    Optional<Conversation> findByStudentAndTeacher(String studentId, Long teacherId);

    List<Conversation> findByStudentId(String studentId);

    List<Conversation> findByTeacherId(Long teacherId);

    ChatMessage saveMessage(ChatMessage message);

    List<ChatMessage> findMessages(String conversationId, int page, int size);

    void markMessagesAsRead(String conversationId, String readerId);
}
