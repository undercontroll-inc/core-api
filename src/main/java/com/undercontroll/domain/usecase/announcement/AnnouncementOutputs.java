package com.undercontroll.domain.usecase.announcement;

import com.undercontroll.domain.gateway.StorageGateway;
import com.undercontroll.domain.model.Announcement;

public final class AnnouncementOutputs {

    private AnnouncementOutputs() {
    }

    public static String imageUrl(Announcement announcement, StorageGateway storageGateway) {
        if (announcement.getImageKey() == null || announcement.getImageKey().isBlank()) {
            return null;
        }
        return storageGateway.announcementImageReadUrl(announcement.getImageKey());
    }

    public static AnnouncementOutput from(Announcement announcement, StorageGateway storageGateway) {
        return new AnnouncementOutput(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                imageUrl(announcement, storageGateway),
                announcement.getType(),
                announcement.getPublishedAt(),
                announcement.getUpdatedAt()
        );
    }
}
