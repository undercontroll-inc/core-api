package com.undercontroll.infrastructure.web.dto;

import com.undercontroll.domain.entity.enums.AnnouncementType;

import java.time.LocalDateTime;

public record AnnouncementDto(
        Integer id,
        String title,
        String content,
        AnnouncementType type,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
}
