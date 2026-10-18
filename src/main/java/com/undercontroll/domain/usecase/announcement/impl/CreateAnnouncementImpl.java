package com.undercontroll.domain.usecase.announcement.impl;

import com.undercontroll.domain.event.AnnouncementCreatedEvent;
import com.undercontroll.domain.gateway.AnnouncementGateway;
import com.undercontroll.domain.gateway.MetricsGateway;
import com.undercontroll.domain.gateway.NotificationGateway;
import com.undercontroll.domain.gateway.StorageGateway;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.model.PresignedUpload;
import com.undercontroll.domain.usecase.announcement.AnnouncementImageKeys;
import com.undercontroll.domain.usecase.announcement.CreateAnnouncementPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateAnnouncementImpl implements CreateAnnouncementPort {

    private final AnnouncementGateway announcementGateway;
    private final NotificationGateway notificationGateway;
    private final MetricsGateway metricsGateway;
    private final StorageGateway storageGateway;

    @Override
    @CacheEvict(value = {"announcements", "lastAnnouncement"}, allEntries = true)
    public Output execute(Input input, String token) {

        Announcement announcement = Announcement.builder()
                .title(input.title())
                .content(input.description())
                .type(input.type())
                .build();

        Announcement announcementCreated = announcementGateway.save(announcement);

        PresignedUpload presignedUpload = input.imageUpload() == null
                ? null
                : storageGateway.presignAnnouncementImageUpload(
                        AnnouncementImageKeys.generate(announcementCreated.getId(), input.imageUpload())
                );

        if (presignedUpload != null) {
            announcementCreated.setImageKey(presignedUpload.fileKey());
            announcementCreated = announcementGateway.save(announcementCreated);
        }

        metricsGateway.incrementAnnouncementCreated();
        log.info("Announcement created id={} type={}", announcementCreated.getId(), announcementCreated.getType());

        notificationGateway.handleAnnouncementCreated(new AnnouncementCreatedEvent(announcementCreated, token));

        return new Output(
                announcementCreated.getId(),
                announcementCreated.getTitle(),
                announcementCreated.getContent(),
                presignedUpload,
                announcementCreated.getType(),
                announcementCreated.getPublishedAt(),
                announcementCreated.getUpdatedAt()
        );
    }
}
