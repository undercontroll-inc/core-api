package com.undercontroll.domain.usecase.announcement;

import com.undercontroll.domain.exception.InvalidUploadException;
import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.model.stockimport.InvoiceFileType;
import com.undercontroll.domain.storage.PresignedUploadUrl;
import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.storage.StorageKeys;

import java.util.Locale;

public final class AnnouncementImages {

    private AnnouncementImages() {
    }

    public static PresignedUploadUrl presign(ObjectStorageGateway storage, Integer announcementId, AnnouncementImageUpload upload) {
        InvoiceFileType type = validate(upload);
        String key = StorageKeys.generate(StorageArea.ANNOUNCEMENT_IMAGES, String.valueOf(announcementId), type);
        return storage.presignUpload(StorageArea.ANNOUNCEMENT_IMAGES, key, type.mimeType(), upload.sizeBytes());
    }

    public static InvoiceFileType validate(AnnouncementImageUpload upload) {
        String declared = upload.contentType() == null ? null : upload.contentType().split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        InvoiceFileType type = InvoiceFileType.fromMimeType(declared)
                .filter(StorageArea.ANNOUNCEMENT_IMAGES.policy()::allows)
                .orElseThrow(() -> new InvalidUploadException("Imagem deve ser JPEG, PNG ou WEBP"));
        if (upload.sizeBytes() == null || upload.sizeBytes() <= 0
                || upload.sizeBytes() > StorageArea.ANNOUNCEMENT_IMAGES.policy().maxBytes()) {
            throw new InvalidUploadException("Imagem deve ter até 5MB");
        }
        return type;
    }

    public static String readUrl(ObjectStorageGateway storage, Announcement announcement) {
        String key = announcement.getImageKey();
        if (key == null || key.isBlank() || !StorageArea.ANNOUNCEMENT_IMAGES.owns(key)) {
            return null;
        }
        return storage.presignRead(StorageArea.ANNOUNCEMENT_IMAGES, key);
    }
}
