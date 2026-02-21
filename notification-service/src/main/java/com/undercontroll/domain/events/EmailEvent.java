package com.undercontroll.domain.events;

import com.undercontroll.domain.enums.EmailEventType;

import java.time.LocalDateTime;

public record EmailEvent(
        String service,
        EmailEventType type,
        Object data,
        LocalDateTime timestamp
) {
}
