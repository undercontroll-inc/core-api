package com.undercontroll.infrastructure.web.dto;

import com.undercontroll.domain.entity.enums.AnnouncementType;

public record UpdateAnnouncementRequest(
        String title,
        String content,
        AnnouncementType type
) {
}
