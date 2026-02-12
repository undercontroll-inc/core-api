package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.GetAnnouncementsPort;
import com.undercontroll.infrastructure.persistence.repository.AnnouncementRepository;
import com.undercontroll.infrastructure.web.dto.AnnouncementDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAnnouncementsImpl implements GetAnnouncementsPort {

    private final AnnouncementRepository announcementRepository;

    @Override
    @Cacheable(value = "announcements", key = "#input.page() + '-' + #input.size()")
    public Output execute(Input input) {
        List<AnnouncementDto> announcements = announcementRepository
                .findAllPaginated(PageRequest.of(input.page(), input.size()))
                .stream()
                .map(this::mapToDto)
                .toList();

        return new Output(announcements);
    }

    private AnnouncementDto mapToDto(Object announcement) {
        return new AnnouncementDto(
                (Integer) ((Object[]) announcement)[0],
                (String) ((Object[]) announcement)[1],
                (String) ((Object[]) announcement)[2],
                null,
                null,
                null
        );
    }
}
