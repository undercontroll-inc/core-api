package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.out.NotificationPort;
import com.undercontroll.domain.port.in.CreateAnnouncementPort;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.events.AnnouncementCreatedEvent;
import com.undercontroll.domain.port.out.AnnouncementRepositoryPort;
import com.undercontroll.domain.port.out.MetricsPort;
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
