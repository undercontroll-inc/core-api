package com.undercontroll.application.usecase;

import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.port.in.GetAnnouncementsPort;
import com.undercontroll.domain.port.out.AnnouncementRepositoryPort;
import com.undercontroll.application.dto.AnnouncementDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAnnouncementsImpl implements GetAnnouncementsPort {

    private final AnnouncementRepositoryPort announcementRepositoryPort;

    @Override
    @Cacheable(value = "announcements", key = "#input.page() + '-' + #input.size()")
    public Output execute(Input input) {
        List<AnnouncementDto> announcements = announcementRepositoryPort
                .findAllPaginated(input.page(), input.size())
                .stream()
                .map(this::mapToDto)
                .toList();

        return new Output(announcements);
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
