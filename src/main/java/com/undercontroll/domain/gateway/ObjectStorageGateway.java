package com.undercontroll.domain.gateway;

import com.undercontroll.domain.storage.PresignedUploadUrl;
import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.storage.StoredObject;

import java.util.Optional;

public interface ObjectStorageGateway {

    PresignedUploadUrl presignUpload(StorageArea area, String key, String contentType, long sizeBytes);

    String presignRead(StorageArea area, String key);

    Optional<StoredObject> stat(StorageArea area, String key);

    byte[] readHead(StorageArea area, String key, int bytes);

    byte[] read(StorageArea area, String key, long maxBytes);

    void put(StorageArea area, String key, byte[] content, String contentType);

    void delete(StorageArea area, String key);
}
