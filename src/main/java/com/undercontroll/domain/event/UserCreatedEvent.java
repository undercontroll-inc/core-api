package com.undercontroll.domain.event;

import java.time.LocalDateTime;

public record UserCreatedEvent(
        String name,
        String email,
        LocalDateTime createdAt
) {
}
