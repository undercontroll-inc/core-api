package com.undercontroll.presentation.dto;

import com.undercontroll.domain.enums.AnnouncementType;

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
