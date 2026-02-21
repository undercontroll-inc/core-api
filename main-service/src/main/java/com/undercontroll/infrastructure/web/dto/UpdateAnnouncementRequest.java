package com.undercontroll.infrastructure.web.dto;

import com.undercontroll.domain.model.enums.AnnouncementType;

public record UpdateAnnouncementRequest(
        String title,
        String content,
        AnnouncementType type
) {
}
