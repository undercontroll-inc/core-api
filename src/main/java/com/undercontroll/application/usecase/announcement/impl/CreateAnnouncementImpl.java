package com.undercontroll.application.usecase.announcement.impl;

import com.undercontroll.application.usecase.announcement.CreateAnnouncementPort;
import com.undercontroll.application.port.NotificationPort;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.events.AnnouncementCreatedEvent;
import com.undercontroll.domain.repository.AnnouncementRepositoryPort;
import com.undercontroll.application.port.MetricsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateAnnouncementImpl implements CreateAnnouncementPort {

    private final AnnouncementRepositoryPort announcementRepositoryPort;
    private final NotificationPort notificationPort;
    private final MetricsPort metricsPort;

    @Override
    @CacheEvict(value = {"announcements", "lastAnnouncement"}, allEntries = true)
    public Output execute(Input input) {
        Announcement announcement = Announcement.builder()
                .title(input.title())
                .content(input.description())
                .type(input.type())
                .build();

        Announcement announcementCreated = announcementRepositoryPort.save(announcement);

        notificationPort.handleAnnouncementCreated(new AnnouncementCreatedEvent(announcementCreated, input.token()));

        metricsPort.incrementAnnouncementCreated();

        return new Output(
                announcementCreated.getId(),
                announcementCreated.getTitle(),
                announcementCreated.getContent(),
                announcementCreated.getType(),
                announcementCreated.getPublishedAt(),
                announcementCreated.getUpdatedAt()
        );
    }
}
