package com.undercontroll.domain.usecase.announcement.impl;

import com.undercontroll.domain.enums.AnnouncementType;
import com.undercontroll.domain.gateway.AnnouncementGateway;
import com.undercontroll.domain.gateway.StorageGateway;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.model.PaginatedResult;
import com.undercontroll.domain.usecase.announcement.AnnouncementOutput;
import com.undercontroll.domain.usecase.announcement.AnnouncementOutputs;
import com.undercontroll.domain.usecase.announcement.GetAnnouncementsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAnnouncementsImpl implements GetAnnouncementsPort {

    private final AnnouncementGateway announcementGateway;
    private final StorageGateway storageGateway;

    @Override
    public Output execute(Integer page, Integer size, AnnouncementType type) {
        PaginatedResult<Announcement> result = announcementGateway
                .findAllPaginated(page, size, type);

        List<AnnouncementOutput> announcements = result.content().stream()
                .map(announcement -> AnnouncementOutputs.from(announcement, storageGateway))
                .toList();

        int totalPages = size > 0
                ? (int) Math.ceil((double) result.totalElements() / size)
                : 0;

        return new Output(announcements, result.totalElements(), totalPages, page, size);
    }
}
