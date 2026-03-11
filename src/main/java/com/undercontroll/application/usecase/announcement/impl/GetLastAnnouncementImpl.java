package com.undercontroll.application.usecase.announcement.impl;

import com.undercontroll.application.usecase.announcement.GetLastAnnouncementPort;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.repository.AnnouncementRepositoryPort;
import com.undercontroll.application.dto.AnnouncementDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetLastAnnouncementImpl implements GetLastAnnouncementPort {

    private final AnnouncementRepositoryPort announcementRepositoryPort;

    @Override
    @Cacheable(value = "lastAnnouncement")
    public Optional<AnnouncementDto> execute() {
        return announcementRepositoryPort.findLastAnnouncement()
                .map(this::mapToDto);
    }

    private AnnouncementDto mapToDto(Announcement announcement) {
        return new AnnouncementDto(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getType(),
                announcement.getPublishedAt(),
                announcement.getUpdatedAt()
        );
    }
}
