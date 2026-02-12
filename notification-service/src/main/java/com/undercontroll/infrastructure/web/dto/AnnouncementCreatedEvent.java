package com.undercontroll.infrastructure.web.dto;

import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

public record AnnouncementCreatedEvent(
        Integer id,
        String title,
        String content,
        AnnouncementType type,
        publishedAt type
) {
}
