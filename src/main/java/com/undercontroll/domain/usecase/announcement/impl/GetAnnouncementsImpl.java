package com.undercontroll.domain.usecase.announcement.impl;

import com.undercontroll.domain.model.PaginatedResult;
import com.undercontroll.domain.usecase.announcement.GetAnnouncementsPort;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.gateway.AnnouncementGateway;
import com.undercontroll.application.dto.announcement.AnnouncementDto;
import com.undercontroll.application.dto.announcement.GetPaginatedAnnouncementResponse;
import com.undercontroll.domain.enums.AnnouncementType;
import com.undercontroll.domain.gateway.StorageGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetAnnouncementsImpl implements GetAnnouncementsPort {

    private final AnnouncementGateway announcementGateway;
    private final StorageGateway storageGateway;

    @Value("${aws.s3.upload-bucket}")
    private String bucket;

    @Value("${aws.s3.read-expiration-minutes:1440}")
    private Long readExpirationMinutes;

    @Override
    public GetPaginatedAnnouncementResponse execute(Integer page, Integer size, AnnouncementType type) {
        PaginatedResult<Announcement> result = announcementGateway
                .findAllPaginated(page, size, type);

        List<AnnouncementDto> announcements = result.content().stream()
                .map(this::mapToDto)
                .toList();

        int totalPages = size > 0
                ? (int) Math.ceil((double) result.totalElements() / size)
                : 0;

        return new GetPaginatedAnnouncementResponse(announcements, result.totalElements(), totalPages, page, size);
    }

    private AnnouncementDto mapToDto(Announcement announcement) {
        return new AnnouncementDto(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                resolveImageUrl(announcement),
                announcement.getType(),
                announcement.getPublishedAt(),
                announcement.getUpdatedAt()
        );
    }

    private String resolveImageUrl(Announcement announcement) {
        if (announcement.getImageKey() == null || announcement.getImageKey().isBlank()) {
            return null;
        }

        return storageGateway.generateReadPresignedUrl(bucket, announcement.getImageKey(), readExpirationMinutes);
    }
}
