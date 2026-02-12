package com.undercontroll.domain.port.in;

import com.undercontroll.domain.entity.enums.AnnouncementType;
import com.undercontroll.infrastructure.web.dto.AnnouncementDto;

import java.time.LocalDateTime;

public interface CreateAnnouncementPort {
    record Input(
            String title,
            String description,
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
