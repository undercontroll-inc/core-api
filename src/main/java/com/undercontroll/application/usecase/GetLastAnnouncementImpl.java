package com.undercontroll.application.usecase;

import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.port.in.GetLastAnnouncementPort;
import com.undercontroll.domain.port.out.AnnouncementRepositoryPort;
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
