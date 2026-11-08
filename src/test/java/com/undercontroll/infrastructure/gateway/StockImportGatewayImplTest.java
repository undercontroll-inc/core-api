package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.enums.StockImportFailureReason;
import com.undercontroll.domain.enums.StockImportItemAction;
import com.undercontroll.domain.enums.StockImportItemWarning;
import com.undercontroll.domain.enums.StockImportMatchType;
import com.undercontroll.domain.enums.StockImportSourceType;
import com.undercontroll.domain.enums.StockImportStage;
import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.domain.enums.StockImportWarning;
import com.undercontroll.domain.model.stockimport.ItemResult;
import com.undercontroll.domain.model.stockimport.MatchCandidate;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.model.stockimport.StockImportItem;
import com.undercontroll.infrastructure.mapper.StockImportPersistenceMapper;
import com.undercontroll.infrastructure.persistence.repository.StockImportFileJpaRepository;
import com.undercontroll.infrastructure.persistence.repository.StockImportJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = false)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import({StockImportGatewayImpl.class, StockImportPersistenceMapper.class})
class StockImportGatewayImplTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");
    private static final Instant LEASE = NOW.plus(Duration.ofMinutes(3));

    @Autowired
    private StockImportGatewayImpl gateway;

    @Autowired
    private StockImportJpaRepository imports;

    @Autowired
    private StockImportFileJpaRepository files;

    @AfterEach
    void cleanUp() {
        files.deleteAll();
        imports.deleteAll();
    }

    private int sequence;

    private StockImport created() {
        return gateway.create(StockImport.builder()
                .storageKey("stock-imports/1/" + (++sequence) + ".jpg")
                .createdBy(1)
                .status(StockImportStatus.PROCESSING)
                .stage(StockImportStage.RECEIVED)
                .sourceType(StockImportSourceType.IMAGE)
                .fileName("nota.jpg")
                .mimeType("image/jpeg")
                .fileSha256("sha")
                .createdAt(NOW)
                .updatedAt(NOW)
                .build());
    }

    private static StockImportItem extracted(int line, String description) {
        return StockImportItem.builder()
                .lineNumber(line)
                .description(description)
                .quantity(new BigDecimal("2.5"))
                .unitPrice(new BigDecimal("4.00"))
                .total(new BigDecimal("10.00"))
                .confidence(0.9)
                .warnings(List.of(StockImportItemWarning.NON_INTEGER_QUANTITY))
                .matchType(StockImportMatchType.FUZZY)
                .matchedComponentId(3)
                .matchConfidence(0.8)
                .candidates(List.of(new MatchCandidate(3, "Fio", null, 10L, 2.0, 0.8)))
                .action(StockImportItemAction.ADD_TO_EXISTING)
                .componentId(3)
                .build();
    }

    @Test
    @DisplayName("lease, stage and complete store the extraction with items")
    void leaseAndComplete() {
        StockImport created = created();
        assertEquals(List.of(created.id()), gateway.findRunnable(NOW, 5));
        assertEquals(created.id(), gateway.findByStorageKey(created.storageKey()).orElseThrow().id());

        StockImport leased = gateway.tryAcquire(created.id(), "w1", NOW, LEASE).orElseThrow();
        assertEquals(1, leased.attemptCount());
        assertTrue(gateway.tryAcquire(created.id(), "w2", NOW, LEASE).isEmpty());
        assertTrue(gateway.findRunnable(NOW, 5).isEmpty());

        gateway.updateStage(created.id(), "w1", StockImportStage.MATCHING, NOW);
        assertEquals(StockImportStage.MATCHING, gateway.findById(created.id()).orElseThrow().stage());

        StockImport ready = leased.toBuilder()
                .status(StockImportStatus.READY)
                .stage(null)
                .supplierName("Eletro Pecas")
                .total(new BigDecimal("20.00"))
                .warnings(List.of(StockImportWarning.ITEMS_TOTAL_MISMATCH))
                .items(List.of(extracted(1, "Fio 1,5mm"), extracted(2, "Fio 2,5mm")))
                .build();
        assertFalse(gateway.complete(ready, "w2", NOW));
        assertTrue(gateway.complete(ready, "w1", NOW));

        StockImport stored = gateway.findById(created.id()).orElseThrow();
        assertEquals(StockImportStatus.READY, stored.status());
        assertNull(stored.stage());
        assertEquals(2, stored.itemCount());
        assertEquals(List.of(StockImportWarning.ITEMS_TOTAL_MISMATCH), stored.warnings());
        StockImportItem first = stored.items().getFirst();
        assertEquals("Fio 1,5mm", first.description());
        assertEquals(0, new BigDecimal("2.5").compareTo(first.quantity()));
        assertEquals(List.of(StockImportItemWarning.NON_INTEGER_QUANTITY), first.warnings());
        assertEquals("Fio", first.candidates().getFirst().name());
        assertEquals(1, gateway.findRecent(StockImportStatus.READY, 10).size());
        assertTrue(gateway.findRecent(StockImportStatus.FAILED, 10).isEmpty());
    }

    @Test
    @DisplayName("save updates, appends and keeps item ids stable")
    void saveSyncsItems() {
        StockImport created = created();
        StockImport leased = gateway.tryAcquire(created.id(), "w1", NOW, LEASE).orElseThrow();
        gateway.complete(leased.toBuilder().status(StockImportStatus.READY)
                .items(List.of(extracted(1, "Fio"))).build(), "w1", NOW);
        StockImport ready = gateway.findById(created.id()).orElseThrow();
        Long itemId = ready.items().getFirst().id();

        List<StockImportItem> items = new ArrayList<>();
        items.add(ready.items().getFirst().toBuilder().description("Fio editado")
                .result(new ItemResult(3, false, 10L, 12L)).build());
        items.add(StockImportItem.builder().lineNumber(2).description("Novo").quantity(BigDecimal.ONE)
                .action(StockImportItemAction.CREATE_NEW).build());
        StockImport saved = gateway.save(ready.toBuilder().status(StockImportStatus.CONFIRMED).confirmedAt(NOW).items(items).build());

        assertEquals(StockImportStatus.CONFIRMED, saved.status());
        assertEquals(2, saved.items().size());
        assertEquals(itemId, saved.items().getFirst().id());
        assertEquals("Fio editado", saved.items().getFirst().description());
        assertEquals(new ItemResult(3, false, 10L, 12L), saved.items().getFirst().result());
        assertTrue(saved.items().get(1).id() != null);
    }

    @Test
    @DisplayName("fail and requeue move the import between FAILED and PROCESSING")
    void failAndRequeue() {
        StockImport created = created();
        gateway.tryAcquire(created.id(), "w1", NOW, LEASE);
        assertFalse(gateway.fail(created.id(), "other", StockImportFailureReason.TIMEOUT, "x", NOW));
        assertTrue(gateway.fail(created.id(), "w1", StockImportFailureReason.UNREADABLE, "blurry", NOW));

        StockImport failed = gateway.findById(created.id()).orElseThrow();
        assertEquals(StockImportStatus.FAILED, failed.status());
        assertEquals(StockImportFailureReason.UNREADABLE, failed.failureReason());
        assertEquals("blurry", failed.failureMessage());

        assertTrue(gateway.requeue(created.id(), NOW));
        StockImport requeued = gateway.findById(created.id()).orElseThrow();
        assertEquals(StockImportStatus.PROCESSING, requeued.status());
        assertEquals(StockImportStage.RECEIVED, requeued.stage());
        assertNull(requeued.failureReason());
        assertEquals(0, requeued.attemptCount());
        assertFalse(gateway.requeue(created.id(), NOW));
    }

    @Test
    @DisplayName("finds confirmed duplicates by access key or file hash and lists stored files to purge once")
    void duplicatesAndPurge() {
        StockImport first = created();
        StockImport leased = gateway.tryAcquire(first.id(), "w1", NOW, LEASE).orElseThrow();
        gateway.complete(leased.toBuilder().status(StockImportStatus.READY).accessKey("1".repeat(44))
                .items(List.of(extracted(1, "Fio"))).build(), "w1", NOW);
        StockImport ready = gateway.findById(first.id()).orElseThrow();
        gateway.save(ready.toBuilder().status(StockImportStatus.CONFIRMED).confirmedAt(NOW).build());
        StockImport second = created();

        assertEquals(first.id(), gateway.findConfirmedDuplicate(second.id(), null, "sha").orElseThrow());
        assertEquals(first.id(), gateway.findConfirmedDuplicate(second.id(), "1".repeat(44), "other").orElseThrow());
        assertTrue(gateway.findConfirmedDuplicate(second.id(), "2".repeat(44), "other").isEmpty());

        assertEquals(0, gateway.purgeLegacyFiles(NOW.plusSeconds(1)));
        List<StockImport> purgeable = gateway.findStoredFilesToPurge(NOW.plusSeconds(1), 10);
        assertEquals(List.of(first.id()), purgeable.stream().map(StockImport::id).toList());
        assertEquals(first.storageKey(), purgeable.getFirst().storageKey());
        gateway.markFilePurged(first.id(), NOW);
        assertTrue(gateway.findStoredFilesToPurge(NOW.plusSeconds(1), 10).isEmpty());
    }
}
