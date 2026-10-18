package com.undercontroll.domain.usecase.announcement;

import com.undercontroll.domain.enums.AnnouncementType;
import com.undercontroll.domain.model.PresignedUpload;

import java.time.LocalDateTime;

public interface UpdateAnnouncementPort {

    Output execute(Integer announcementId, Input input);

    record Input(
            String title,
            String content,
            AnnouncementImageUpload imageUpload,
            Boolean removeImage,
            AnnouncementType type
    ) {
    }

    record Output(
            Integer id,
            String title,
            String content,
            String imageUrl,
            PresignedUpload imageUpload,
            AnnouncementType type,
            LocalDateTime publishedAt,
            LocalDateTime updatedAt
    ) {
    }
}
