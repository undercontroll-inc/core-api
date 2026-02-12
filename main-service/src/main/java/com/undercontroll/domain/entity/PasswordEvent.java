package com.undercontroll.domain.entity;

import com.undercontroll.domain.entity.enums.PasswordEventStatus;
import com.undercontroll.domain.entity.enums.PasswordEventType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
public class PasswordEvent {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private PasswordEventType type;

    @Enumerated(EnumType.STRING)
    private PasswordEventStatus status;

    private String value;

    @NotNull
    private String userPhone;

    private String userAgent;

    @CreationTimestamp
    private LocalDateTime createdAt;

}
