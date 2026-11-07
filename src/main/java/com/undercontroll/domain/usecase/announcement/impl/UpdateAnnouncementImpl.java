package com.undercontroll.domain.usecase.announcement.impl;

import com.undercontroll.domain.exception.AnnouncementNotFoundException;
import com.undercontroll.domain.exception.InvalidAnnouncementException;
import com.undercontroll.domain.gateway.AnnouncementGateway;
import com.undercontroll.domain.gateway.StorageGateway;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.model.PresignedUpload;
import com.undercontroll.domain.usecase.announcement.AnnouncementImageKeys;
import com.undercontroll.domain.usecase.announcement.AnnouncementOutputs;
import com.undercontroll.domain.usecase.announcement.UpdateAnnouncementPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateAnnouncementImpl implements UpdateAnnouncementPort {

    private final AnnouncementGateway announcementGateway;
    private final StorageGateway storageGateway;

    @Override
    @CacheEvict(value = {"announcements", "lastAnnouncement"}, allEntries = true)
    public Output execute(Integer announcementId, Input input) {
        if (input.imageUpload() != null && Boolean.TRUE.equals(input.removeImage())) {
            throw new InvalidAnnouncementException("Cannot upload and remove an image at the same time");
        }

        Announcement announcement = announcementGateway
                .findById(announcementId)
                .orElseThrow(() -> new AnnouncementNotFoundException(
                        "Announcement with id " + announcementId + " not found"
                ));

        if (input.title() != null) {
            announcement.setTitle(input.title());
        }

        if (input.content() != null) {
            announcement.setContent(input.content());
        }

        if (Boolean.TRUE.equals(input.removeImage())) {
            announcement.setImageKey(null);
        }

        if (input.type() != null) {
            announcement.setType(input.type());
        }

        PresignedUpload imageUpload = input.imageUpload() == null
                ? null
                : storageGateway.presignAnnouncementImageUpload(
                        AnnouncementImageKeys.generate(announcement.getId(), input.imageUpload())
                );

        if (imageUpload != null) {
            announcement.setImageKey(imageUpload.fileKey());
        }

        Announcement saved = announcementGateway.save(announcement);

        return new Output(
                saved.getId(),
                saved.getTitle(),
                saved.getContent(),
                AnnouncementOutputs.imageUrl(saved, storageGateway),
                imageUpload,
                saved.getType(),
                saved.getPublishedAt(),
                saved.getUpdatedAt()
        );
    }
}
