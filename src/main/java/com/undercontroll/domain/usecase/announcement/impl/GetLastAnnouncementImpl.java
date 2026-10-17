package com.undercontroll.domain.usecase.announcement.impl;

import com.undercontroll.domain.usecase.announcement.GetLastAnnouncementPort;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.gateway.AnnouncementGateway;
import com.undercontroll.application.dto.announcement.AnnouncementDto;
import com.undercontroll.domain.gateway.StorageGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetLastAnnouncementImpl implements GetLastAnnouncementPort {

    private final AnnouncementGateway announcementGateway;
    private final StorageGateway storageGateway;

    @Value("${aws.s3.upload-bucket}")
    private String bucket;

    @Value("${aws.s3.read-expiration-minutes:1440}")
    private Long readExpirationMinutes;

    @Override
    public Optional<AnnouncementDto> execute() {
        return announcementGateway.findLastAnnouncement()
                .map(this::mapToDto);
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
