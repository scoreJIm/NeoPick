package com.neopick.adapter.persistence.repository;

import com.neopick.adapter.persistence.entity.ChatMessageJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessageJpaEntity, UUID> {

    List<ChatMessageJpaEntity> findByConversationIdOrderBySentAtDesc(String conversationId, Pageable pageable);

    @Modifying
    @Query("UPDATE ChatMessageJpaEntity m SET m.read = true "
            + "WHERE m.conversationId = :conversationId AND m.receiverId = :readerId AND m.read = false")
    void markAsReadByConversationIdAndReceiverId(@Param("conversationId") String conversationId,
                                                 @Param("readerId") String readerId);
}
