package com.undercontroll.infrastructure.persistence.entity;

import com.undercontroll.domain.model.chat.Conversation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "chat_conversation",
        uniqueConstraints = @UniqueConstraint(name = "uq_chat_conversation_user_public", columnNames = {"user_id", "public_id"}),
        indexes = @Index(name = "ix_chat_conversation_recent", columnList = "user_id, last_message_at, id")
)
public class ChatConversationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "public_id", nullable = false, length = 64)
    private String publicId;

    @Column(length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "title_source", nullable = false, length = 10)
    private Conversation.TitleSource titleSource;

    @Column(name = "last_message_preview", length = 160)
    private String lastMessagePreview;

    @Column(name = "message_count", nullable = false)
    private int messageCount;

    @Column(name = "last_seq", nullable = false)
    private int lastSeq;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_message_at", nullable = false)
    private Instant lastMessageAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
