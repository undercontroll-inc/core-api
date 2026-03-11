package com.undercontroll.application.usecase.announcement.impl;

import com.undercontroll.application.usecase.announcement.DeleteAnnouncementPort;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.exception.AnnouncementNotFoundException;
import com.undercontroll.domain.repository.AnnouncementRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteAnnouncementImpl implements DeleteAnnouncementPort {

    private final AnnouncementRepositoryPort announcementRepositoryPort;

    @Override
    @CacheEvict(value = {"announcements", "lastAnnouncement"}, allEntries = true)
    public void execute(Input input) {
        Announcement announcement = announcementRepositoryPort
                .findById(input.id())
                .orElseThrow(() -> new AnnouncementNotFoundException(
                        "Announcement with id " + input.id() + " not found"
                ));

        announcementRepositoryPort.deleteById(announcement.getId());
    }
}
