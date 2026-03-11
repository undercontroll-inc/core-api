package com.undercontroll.application.usecase.announcement;

import com.undercontroll.domain.enums.AnnouncementType;

import java.time.LocalDateTime;

public interface UpdateAnnouncementPort {
    record Input(
            Integer id,
            String title,
            String content,
            AnnouncementType type
    ) {}

    record Output(
            Integer id,
            String title,
            String content,
            AnnouncementType type,
            LocalDateTime publishedAt,
            LocalDateTime updatedAt
    ) {}

    Output execute(Input input);
}
