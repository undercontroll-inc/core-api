package com.undercontroll.domain.usecase.announcement;

import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.model.Announcement;

public final class AnnouncementOutputs {

    private AnnouncementOutputs() {
    }

    public static String imageUrl(Announcement announcement, ObjectStorageGateway storageGateway) {
        return AnnouncementImages.readUrl(storageGateway, announcement);
    }

    public static AnnouncementOutput from(Announcement announcement, ObjectStorageGateway storageGateway) {
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
