package com.undercontroll.infrastructure.persistence.entity;

import com.undercontroll.domain.model.chat.AnaReply;
import com.undercontroll.domain.model.chat.ChatMessage;
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
        name = "chat_message",
        uniqueConstraints = @UniqueConstraint(name = "uq_chat_message_seq", columnNames = {"conversation_id", "seq"})
)
public class ChatMessageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(nullable = false)
    private int seq;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChatMessage.Role role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AnaReply.State state;

    @Column(length = 40)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
