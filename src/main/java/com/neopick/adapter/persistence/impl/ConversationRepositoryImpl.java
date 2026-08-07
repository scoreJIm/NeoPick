package com.neopick.adapter.persistence.impl;

import com.neopick.adapter.persistence.entity.ChatMessageJpaEntity;
import com.neopick.adapter.persistence.entity.ConversationJpaEntity;
import com.neopick.adapter.persistence.repository.ChatMessageJpaRepository;
import com.neopick.adapter.persistence.repository.ConversationJpaRepository;
import com.neopick.domain.message.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ConversationRepositoryImpl implements ConversationRepository {

    private final ConversationJpaRepository convRepo;
    private final ChatMessageJpaRepository msgRepo;

    public ConversationRepositoryImpl(ConversationJpaRepository convRepo,
                                       ChatMessageJpaRepository msgRepo) {
        this.convRepo = convRepo;
        this.msgRepo = msgRepo;
    }

    @Override
    public Conversation save(Conversation conversation) {
        ConversationJpaEntity e = toEntity(conversation);
        return toDomain(convRepo.save(e));
    }

    @Override
    public Optional<Conversation> findById(ConversationId id) {
        return convRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<Conversation> findByStudentAndTeacher(String studentId, Long teacherId) {
        return convRepo.findByStudentIdAndTeacherId(studentId, teacherId).map(this::toDomain);
    }

    @Override
    public List<Conversation> findByStudentId(String studentId) {
        return convRepo.findByStudentIdOrderByLastMessageAtDesc(studentId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Conversation> findByTeacherId(Long teacherId) {
        return convRepo.findByTeacherIdOrderByLastMessageAtDesc(teacherId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public ChatMessage saveMessage(ChatMessage message) {
        ChatMessageJpaEntity e = new ChatMessageJpaEntity();
        e.setId(message.getId());
        e.setConversationId(message.getConversationId());
        e.setSenderId(message.getSenderId());
        e.setReceiverId(message.getReceiverId());
        e.setContent(message.getContent());
        e.setMessageType(message.getMessageType().name());
        e.setRead(message.isRead());
        e.setSentAt(message.getSentAt());
        ChatMessageJpaEntity saved = msgRepo.save(e);
        return new ChatMessage(saved.getId(), saved.getConversationId(),
                saved.getSenderId(), saved.getReceiverId(), saved.getContent(),
                MessageType.valueOf(saved.getMessageType()));
    }

    @Override
    public List<ChatMessage> findMessages(String conversationId, int page, int size) {
        return msgRepo.findByConversationIdOrderBySentAtDesc(conversationId, PageRequest.of(page, size))
                .stream().map(e -> new ChatMessage(e.getId(), e.getConversationId(),
                        e.getSenderId(), e.getReceiverId(), e.getContent(),
                        MessageType.valueOf(e.getMessageType()))).toList();
    }

    private ConversationJpaEntity toEntity(Conversation c) {
        ConversationJpaEntity e = new ConversationJpaEntity();
        e.setId(c.getId().value());
        e.setStudentId(c.getStudentId());
        e.setTeacherId(c.getTeacherId());
        e.setLastMessageContent(c.getLastMessageContent());
        e.setLastMessageAt(c.getLastMessageAt());
        e.setCreatedAt(c.getCreatedAt());
        return e;
    }

    private Conversation toDomain(ConversationJpaEntity e) {
        return new Conversation(new ConversationId(e.getId()), e.getStudentId(), e.getTeacherId());
    }
}
