package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.storage.UploadIntent;
import com.undercontroll.infrastructure.persistence.repository.StorageUploadJpaRepository;
import com.undercontroll.infrastructure.scheduling.StorageUploadSweeper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DataJpaTest(showSql = false)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(UploadIntentGatewayImpl.class)
class UploadIntentGatewayImplTest {

    private static final Instant NOW = Instant.parse("2026-11-08T12:00:00Z");

    @Autowired
    private UploadIntentGatewayImpl gateway;

    @Autowired
    private StorageUploadJpaRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    private UploadIntent pending(String key, Instant expiresAt) {
        return gateway.save(new UploadIntent(UUID.randomUUID(), StorageArea.ORDER_PHOTOS, key, 7, "12", "DEVICE", "a.jpg",
                "image/jpeg", 10, UploadIntent.Status.PENDING, expiresAt, NOW, null));
    }

    @Test
    @DisplayName("completes only once and counts only live pending uploads of the same reference")
    void lifecycle() {
        UploadIntent first = pending("order-photos/12/a.jpg", NOW.plusSeconds(300));
        pending("order-photos/12/b.jpg", NOW.plusSeconds(300));
        pending("order-photos/12/c.jpg", NOW.minusSeconds(1));

        assertEquals(2, gateway.countPending(StorageArea.ORDER_PHOTOS, "12", NOW));
        assertTrue(gateway.markCompleted(first.id(), NOW));
        assertFalse(gateway.markCompleted(first.id(), NOW));
        assertEquals(UploadIntent.Status.COMPLETED, gateway.findById(first.id()).orElseThrow().status());
        assertEquals(1, gateway.countPending(StorageArea.ORDER_PHOTOS, "12", NOW));
        assertEquals(0, gateway.countPending(StorageArea.ORDER_PHOTOS, "13", NOW));
    }

    @Test
    @DisplayName("the sweeper deletes abandoned uploads and their objects after the grace period")
    void sweep() {
        UploadIntent abandoned = pending("order-photos/12/old.jpg", NOW.minus(Duration.ofHours(2)));
        UploadIntent recent = pending("order-photos/12/new.jpg", NOW.minus(Duration.ofMinutes(10)));
        ObjectStorageGateway storage = mock(ObjectStorageGateway.class);

        new StorageUploadSweeper(gateway, storage, Clock.fixed(NOW, ZoneOffset.UTC)).sweep();

        verify(storage).delete(StorageArea.ORDER_PHOTOS, "order-photos/12/old.jpg");
        assertTrue(gateway.findById(abandoned.id()).isEmpty());
        assertTrue(gateway.findById(recent.id()).isPresent());
    }
}
