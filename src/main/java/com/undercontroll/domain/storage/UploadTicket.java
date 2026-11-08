package com.undercontroll.domain.storage;

import java.util.UUID;

public record UploadTicket(UUID uploadId, PresignedUploadUrl upload) {
}
