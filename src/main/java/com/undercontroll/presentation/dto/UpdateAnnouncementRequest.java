package com.undercontroll.presentation.dto;

import com.undercontroll.domain.enums.AnnouncementType;

public record UpdateAnnouncementRequest(
        String title,
        String content,
        AnnouncementType type
) {
}
