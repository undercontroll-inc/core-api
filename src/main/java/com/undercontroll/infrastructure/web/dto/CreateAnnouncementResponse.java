package com.undercontroll.infrastructure.web.dto;

import com.undercontroll.domain.model.enums.AnnouncementType;

import java.time.LocalDateTime;

public record CreateAnnouncementResponse(
        Integer id,
        String title,
        String content,
        AnnouncementType type,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
}
