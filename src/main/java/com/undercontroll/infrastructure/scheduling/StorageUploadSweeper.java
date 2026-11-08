package com.undercontroll.infrastructure.scheduling;

import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.gateway.UploadIntentGateway;
import com.undercontroll.domain.storage.UploadIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageUploadSweeper {

    static final Duration GRACE = Duration.ofHours(1);
    static final int BATCH = 200;

    private final UploadIntentGateway intents;
    private final ObjectStorageGateway storage;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${undercontroll.storage.sweep-interval:15m}", initialDelayString = "2m")
    public void sweep() {
        try {
            List<UploadIntent> expired = intents.findExpiredPending(clock.instant().minus(GRACE), BATCH);
            for (UploadIntent intent : expired) {
                storage.delete(intent.area(), intent.objectKey());
                intents.delete(intent.id());
            }
            if (!expired.isEmpty()) {
                log.info("Removed {} abandoned upload(s)", expired.size());
            }
        } catch (RuntimeException ex) {
            log.warn("Upload sweep failed: {}", ex.toString());
        }
    }
}
