package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.infrastructure.persistence.entity.ChatMessageJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageJpaRepository extends JpaRepository<ChatMessageJpaEntity, Long> {

    @Query("SELECT m FROM ChatMessageJpaEntity m WHERE m.conversationId = :conversationId ORDER BY m.seq DESC")
    List<ChatMessageJpaEntity> findLatest(@Param("conversationId") Long conversationId, Pageable page);

    @Query("""
            SELECT m FROM ChatMessageJpaEntity m
            WHERE m.conversationId = :conversationId AND m.seq < :beforeSeq
            ORDER BY m.seq DESC
            """)
    List<ChatMessageJpaEntity> findBefore(
            @Param("conversationId") Long conversationId,
            @Param("beforeSeq") int beforeSeq,
            Pageable page
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ChatMessageJpaEntity m WHERE m.conversationId IN :conversationIds")
    int deleteByConversationIds(@Param("conversationIds") List<Long> conversationIds);
}
