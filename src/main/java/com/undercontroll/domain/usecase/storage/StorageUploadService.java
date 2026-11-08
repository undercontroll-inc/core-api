package com.undercontroll.domain.usecase.storage;

import com.undercontroll.domain.exception.InvalidUploadException;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.gateway.UploadIntentGateway;
import com.undercontroll.domain.model.stockimport.InvoiceFileType;
import com.undercontroll.domain.storage.PresignedUploadUrl;
import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.storage.StorageKeys;
import com.undercontroll.domain.storage.StoredObject;
import com.undercontroll.domain.storage.UploadIntent;
import com.undercontroll.domain.storage.UploadTicket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageUploadService {

    static final int MAGIC_BYTES = 16;
    static final int MAX_NAME = 255;

    private final ObjectStorageGateway storage;
    private final UploadIntentGateway intents;
    private final CurrentUserIdPort currentUserIdPort;
    private final Clock clock;

    public UploadTicket request(StorageArea area, String contentType, Long sizeBytes, String originalName,
                                String reference, String kind) {
        if (!area.clientWritable()) {
            throw new IllegalStateException(area + " does not accept client uploads");
        }
        InvoiceFileType type = InvoiceFileType.fromMimeType(mediaType(contentType))
                .filter(area.policy()::allows)
                .orElseThrow(() -> new InvalidUploadException("Tipo de arquivo não aceito: " + contentType));
        if (sizeBytes == null || sizeBytes <= 0) {
            throw new InvalidUploadException("Informe o tamanho do arquivo em bytes");
        }
        if (sizeBytes > area.policy().maxBytes()) {
            throw new InvalidUploadException("Arquivo maior que " + area.policy().maxBytes() / (1024 * 1024) + "MB");
        }
        Integer owner = currentUserIdPort.require();
        String key = StorageKeys.generate(area, reference == null ? String.valueOf(owner) : reference, type);
        PresignedUploadUrl upload = storage.presignUpload(area, key, type.mimeType(), sizeBytes);
        Instant now = clock.instant();
        UploadIntent intent = intents.save(new UploadIntent(
                UUID.randomUUID(), area, key, owner, reference, kind, clip(originalName), type.mimeType(), sizeBytes,
                UploadIntent.Status.PENDING, upload.expiresAt(), now, null));
        log.info("Upload requested id={} area={} bytes={}", intent.id(), area, sizeBytes);
        return new UploadTicket(intent.id(), upload);
    }

    public UploadIntent complete(UUID uploadId, StorageArea area, String reference) {
        UploadIntent intent = intents.findById(uploadId)
                .filter(found -> found.area() == area)
                .filter(found -> found.ownerUserId().equals(currentUserIdPort.require()))
                .filter(found -> reference == null || reference.equals(found.reference()))
                .orElseThrow(() -> new InvalidUploadException("Upload não encontrado"));
        if (intent.status() == UploadIntent.Status.COMPLETED) {
            return intent;
        }
        if (intent.status() == UploadIntent.Status.REJECTED) {
            throw new InvalidUploadException("Upload recusado; envie o arquivo de novo");
        }
        Instant now = clock.instant();
        Optional<StoredObject> stored = storage.stat(area, intent.objectKey());
        if (stored.isEmpty()) {
            if (now.isAfter(intent.expiresAt())) {
                reject(intent, now, "O prazo para enviar o arquivo terminou; peça um novo link");
            }
            throw new InvalidUploadException("O arquivo ainda não foi enviado para o link de upload");
        }
        if (stored.get().size() != intent.sizeBytes() || stored.get().size() > area.policy().maxBytes()) {
            reject(intent, now, "O tamanho do arquivo enviado não confere");
        }
        Optional<InvoiceFileType> detected = InvoiceFileType.detect(storage.readHead(area, intent.objectKey(), MAGIC_BYTES));
        if (detected.isEmpty() || !detected.get().mimeType().equals(intent.contentType())) {
            reject(intent, now, "O conteúdo do arquivo não confere com o tipo informado");
        }
        intents.markCompleted(intent.id(), now);
        log.info("Upload completed id={} area={} bytes={}", intent.id(), area, stored.get().size());
        return new UploadIntent(intent.id(), intent.area(), intent.objectKey(), intent.ownerUserId(), intent.reference(),
                intent.kind(), intent.originalName(), intent.contentType(), intent.sizeBytes(),
                UploadIntent.Status.COMPLETED, intent.expiresAt(), intent.createdAt(), now);
    }

    private void reject(UploadIntent intent, Instant now, String reason) {
        storage.delete(intent.area(), intent.objectKey());
        intents.markRejected(intent.id(), now);
        log.warn("Upload rejected id={} area={} reason={}", intent.id(), intent.area(), reason);
        throw new InvalidUploadException(reason);
    }

    private static String mediaType(String contentType) {
        if (contentType == null) {
            return null;
        }
        return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private static String clip(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.strip();
        return trimmed.length() > MAX_NAME ? trimmed.substring(0, MAX_NAME) : trimmed;
    }
}
