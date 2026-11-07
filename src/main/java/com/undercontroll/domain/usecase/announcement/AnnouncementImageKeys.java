package com.undercontroll.domain.usecase.announcement;

import java.util.Locale;
import java.util.UUID;

public final class AnnouncementImageKeys {

    private AnnouncementImageKeys() {
    }

    public static String generate(Integer announcementId, AnnouncementImageUpload upload) {
        return "announcements/%d/%s.%s".formatted(
                announcementId,
                UUID.randomUUID(),
                resolveExtension(upload.originalName(), upload.contentType())
        );
    }

    static String resolveExtension(String originalName, String contentType) {
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

    private static String extensionFromName(String originalName) {
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
