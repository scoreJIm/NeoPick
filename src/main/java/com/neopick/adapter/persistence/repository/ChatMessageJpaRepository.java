package com.neopick.adapter.persistence.repository;

import com.neopick.adapter.persistence.entity.ChatMessageJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessageJpaEntity, UUID> {

    List<ChatMessageJpaEntity> findByConversationIdOrderBySentAtDesc(String conversationId, Pageable pageable);
}
