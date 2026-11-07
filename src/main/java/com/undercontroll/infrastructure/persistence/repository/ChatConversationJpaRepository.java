package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.infrastructure.persistence.entity.ChatConversationJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationJpaRepository extends JpaRepository<ChatConversationJpaEntity, Long> {

    Optional<ChatConversationJpaEntity> findByUserIdAndPublicIdAndDeletedAtIsNull(Integer userId, String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ChatConversationJpaEntity c WHERE c.userId = :userId AND c.publicId = :publicId")
    Optional<ChatConversationJpaEntity> findForUpdate(@Param("userId") Integer userId, @Param("publicId") String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ChatConversationJpaEntity c WHERE c.id = :id")
    Optional<ChatConversationJpaEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT c FROM ChatConversationJpaEntity c
            WHERE c.userId = :userId AND c.deletedAt IS NULL
            ORDER BY c.lastMessageAt DESC, c.id DESC
            """)
    List<ChatConversationJpaEntity> findRecent(@Param("userId") Integer userId, Pageable page);

    @Query("""
            SELECT c FROM ChatConversationJpaEntity c
            WHERE c.userId = :userId AND c.deletedAt IS NULL
              AND (c.lastMessageAt < :at OR (c.lastMessageAt = :at AND c.id < :id))
            ORDER BY c.lastMessageAt DESC, c.id DESC
            """)
    List<ChatConversationJpaEntity> findRecentAfter(
            @Param("userId") Integer userId,
            @Param("at") Instant at,
            @Param("id") Long id,
            Pageable page
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ChatConversationJpaEntity c SET c.deletedAt = :now, c.updatedAt = :now
            WHERE c.userId = :userId AND c.publicId = :publicId AND c.deletedAt IS NULL
            """)
    int softDelete(@Param("userId") Integer userId, @Param("publicId") String publicId, @Param("now") Instant now);

    @Query("SELECT c.id FROM ChatConversationJpaEntity c WHERE c.deletedAt IS NOT NULL AND c.deletedAt < :before")
    List<Long> findDeletedBefore(@Param("before") Instant before, Pageable page);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ChatConversationJpaEntity c WHERE c.id IN :ids")
    int deleteByIds(@Param("ids") List<Long> ids);
}
