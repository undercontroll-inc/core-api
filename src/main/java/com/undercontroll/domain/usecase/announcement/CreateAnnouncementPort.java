package com.undercontroll.domain.usecase.announcement;

import com.undercontroll.domain.enums.AnnouncementType;
import com.undercontroll.domain.model.PresignedUpload;

import java.time.LocalDateTime;

public interface CreateAnnouncementPort {

    Output execute(Input input, String token);

    record Input(String title, String description, AnnouncementImageUpload imageUpload, AnnouncementType type) {
    }

    record Output(
            Integer id,
            String title,
            String content,
            PresignedUpload imageUpload,
            AnnouncementType type,
            LocalDateTime publishedAt,
            LocalDateTime updatedAt
    ) {
    }
}
