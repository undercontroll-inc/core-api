package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.PresignedUpload;

public interface StorageGateway {

    PresignedUpload presignAnnouncementImageUpload(String key);

    String announcementImageReadUrl(String key);

    void storeOrderExport(String key, byte[] pdf);
}
