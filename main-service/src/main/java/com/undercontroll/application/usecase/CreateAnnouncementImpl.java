package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.CreateAnnouncementPort;
import com.undercontroll.domain.entity.Announcement;
import com.undercontroll.infrastructure.persistence.repository.AnnouncementRepository;
import com.undercontroll.application.service.MetricsService;
import com.undercontroll.infrastructure.messaging.event.AnnouncementCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateAnnouncementImpl implements CreateAnnouncementPort {

    private final AnnouncementRepository announcementRepository;
    private final ApplicationEventPublisher publisher;
    private final MetricsService metricsService;

    @Override
    @CacheEvict(value = {"announcements", "lastAnnouncement"}, allEntries = true)
    public Output execute(Input input) {
        Announcement announcement = Announcement.builder()
                .title(input.title())
                .content(input.description())
                .type(input.type())
                .build();

        Announcement announcementCreated = announcementRepository.save(announcement);

        publisher.publishEvent(new AnnouncementCreatedEvent(this, announcementCreated));

        metricsService.incrementAnnouncementCreated();

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
