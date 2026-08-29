package com.neopick.adapter.persistence.repository;

import com.neopick.adapter.persistence.entity.ConversationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationJpaRepository extends JpaRepository<ConversationJpaEntity, UUID> {

    Optional<ConversationJpaEntity> findByStudentIdAndTeacherId(String studentId, Long teacherId);

    List<ConversationJpaEntity> findByStudentIdOrderByLastMessageAtDesc(String studentId);

    List<ConversationJpaEntity> findByTeacherIdOrderByLastMessageAtDesc(Long teacherId);
}
