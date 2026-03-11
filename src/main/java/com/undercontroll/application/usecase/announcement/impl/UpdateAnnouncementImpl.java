package com.undercontroll.application.usecase.announcement.impl;

import com.undercontroll.application.usecase.announcement.UpdateAnnouncementPort;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.exception.AnnouncementNotFoundException;
import com.undercontroll.domain.repository.AnnouncementRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateAnnouncementImpl implements UpdateAnnouncementPort {

    private final AnnouncementRepositoryPort announcementRepositoryPort;

    @Override
    @CacheEvict(value = {"announcements", "lastAnnouncement"}, allEntries = true)
    public Output execute(Input input) {
        Announcement announcement = announcementRepositoryPort
                .findById(input.id())
                .orElseThrow(() -> new AnnouncementNotFoundException(
                        "Announcement with id " + input.id() + " not found"
                ));

        if (input.title() != null) {
            announcement.setTitle(input.title());
        }

        if (input.content() != null) {
            announcement.setContent(input.content());
        }

        if (input.type() != null) {
            announcement.setType(input.type());
        }

        Announcement saved = announcementRepositoryPort.save(announcement);

        return new Output(
                saved.getId(),
                saved.getTitle(),
                saved.getContent(),
                saved.getType(),
                saved.getPublishedAt(),
                saved.getUpdatedAt()
        );
    }
}
