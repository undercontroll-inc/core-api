package com.undercontroll.domain.usecase.announcement;

import com.undercontroll.domain.enums.AnnouncementType;

import java.time.LocalDateTime;

public record AnnouncementOutput(
        Integer id,
        String title,
        String content,
        String imageUrl,
        AnnouncementType type,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
}
