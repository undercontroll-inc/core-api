package com.undercontroll.infrastructure.persistence.entity;

import com.undercontroll.domain.model.PasswordEvent;
import com.undercontroll.domain.enums.PasswordEventStatus;
import com.undercontroll.domain.enums.PasswordEventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "password_event")
public class PasswordEventJpaEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private PasswordEventType type;

    @Enumerated(EnumType.STRING)
    private PasswordEventStatus status;

    private String value;

    private String userPhone;

    private String userAgent;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public PasswordEvent toDomain() {
        return PasswordEvent.builder()
                .id(id)
                .type(type)
                .status(status)
                .value(value)
                .userPhone(userPhone)
                .userAgent(userAgent)
                .createdAt(createdAt)
                .build();
    }

    public static PasswordEventJpaEntity fromDomain(PasswordEvent passwordEvent) {
        if (passwordEvent == null) return null;
        return PasswordEventJpaEntity.builder()
                .id(passwordEvent.getId())
                .type(passwordEvent.getType())
                .status(passwordEvent.getStatus())
                .value(passwordEvent.getValue())
                .userPhone(passwordEvent.getUserPhone())
                .userAgent(passwordEvent.getUserAgent())
                .createdAt(passwordEvent.getCreatedAt())
                .build();
    }

}
