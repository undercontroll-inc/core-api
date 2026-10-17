package com.undercontroll.domain.usecase.announcement.impl;

import com.undercontroll.domain.usecase.announcement.CreateAnnouncementPort;
import com.undercontroll.domain.gateway.NotificationGateway;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.infrastructure.events.AnnouncementCreatedEvent;
import com.undercontroll.domain.gateway.AnnouncementGateway;
import com.undercontroll.domain.gateway.MetricsGateway;
import com.undercontroll.domain.gateway.StorageGateway;
import com.undercontroll.application.dto.announcement.CreateAnnouncementRequest;
import com.undercontroll.application.dto.announcement.CreateAnnouncementResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateAnnouncementImpl implements CreateAnnouncementPort {

    @Value("${aws.s3.upload-bucket}")
    private String bucket;

    @Value("${aws.s3.upload-expiration-minutes:15}")
    private Integer uploadExpirationMinutes;

    private final AnnouncementGateway announcementGateway;
    private final NotificationGateway notificationGateway;
    private final MetricsGateway metricsGateway;
    private final StorageGateway storageGateway;

    @Override
    @CacheEvict(value = {"announcements", "lastAnnouncement"}, allEntries = true)
    public CreateAnnouncementResponse execute(CreateAnnouncementRequest request, String token) {

        Announcement announcement = Announcement.builder()
                .title(request.title())
                .content(request.description())
                .type(request.type())
                .build();

        Announcement announcementCreated = announcementGateway.save(announcement);

        final var presignedUpload = request.imageUpload() == null
                ? null
                : storageGateway.generatePresignedUploadUrl(
                        bucket,
                        generateKey(announcementCreated.getId(), request.imageUpload().originalName(), request.imageUpload().contentType()),
                        uploadExpirationMinutes
                );

        if (presignedUpload != null) {
            announcementCreated.setImageKey(presignedUpload.fileKey());
            announcementCreated = announcementGateway.save(announcementCreated);
        }

        metricsGateway.incrementAnnouncementCreated();
        log.info("Announcement created id={} type={}", announcementCreated.getId(), announcementCreated.getType());

        notificationGateway.handleAnnouncementCreated(new AnnouncementCreatedEvent(announcementCreated, token));

        return new CreateAnnouncementResponse(
                announcementCreated.getId(),
                announcementCreated.getTitle(),
                announcementCreated.getContent(),
                presignedUpload,
                announcementCreated.getType(),
                announcementCreated.getPublishedAt(),
                announcementCreated.getUpdatedAt()
        );
    }

    private String generateKey(Integer announcementId, String originalName, String contentType) {
        return "announcements/%d/%s.%s".formatted(
                announcementId,
                UUID.randomUUID(),
                resolveExtension(originalName, contentType)
        );
    }

    private String resolveExtension(String originalName, String contentType) {
        String extension = extensionFromName(originalName);

        if (!extension.isBlank()) {
            return extension;
        }

        if (contentType == null || contentType.isBlank()) {
            return "bin";
        }

        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "bin";
        };
    }

    private String extensionFromName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "";
        }

        int dotIndex = originalName.lastIndexOf('.');

        if (dotIndex < 0 || dotIndex == originalName.length() - 1) {
            return "";
        }

        return originalName.substring(dotIndex + 1).replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
    }
}
