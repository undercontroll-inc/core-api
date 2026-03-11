package com.undercontroll.domain.model;

import com.undercontroll.domain.enums.PasswordEventStatus;
import com.undercontroll.domain.enums.PasswordEventType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordEvent {

    private UUID id;

    private PasswordEventType type;

    private PasswordEventStatus status;

    private String value;

    private String userPhone;

    private String userAgent;

    private LocalDateTime createdAt;

}
