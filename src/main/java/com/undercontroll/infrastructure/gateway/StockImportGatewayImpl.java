package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.enums.StockImportFailureReason;
import com.undercontroll.domain.enums.StockImportStage;
import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.domain.gateway.StockImportGateway;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.model.stockimport.StockImportItem;
import com.undercontroll.infrastructure.mapper.StockImportPersistenceMapper;
import com.undercontroll.infrastructure.persistence.entity.StockImportFileJpaEntity;
import com.undercontroll.infrastructure.persistence.entity.StockImportItemJpaEntity;
import com.undercontroll.infrastructure.persistence.entity.StockImportJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.StockImportFileJpaRepository;
import com.undercontroll.infrastructure.persistence.repository.StockImportJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class StockImportGatewayImpl implements StockImportGateway {

    private static final String MAX_MESSAGE = "%.500s";

    private final StockImportJpaRepository imports;
    private final StockImportFileJpaRepository files;
    private final StockImportPersistenceMapper mapper;

    @Override
    @Transactional
    public StockImport create(StockImport stockImport, byte[] content) {
        StockImportJpaEntity entity = new StockImportJpaEntity();
        mapper.applyHeader(stockImport, entity);
        entity.setNextRunAt(stockImport.createdAt());
        StockImportJpaEntity saved = imports.saveAndFlush(entity);
        files.save(StockImportFileJpaEntity.builder().importId(saved.getId()).content(content).build());
        return mapper.toDomain(saved, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StockImport> findById(Long importId) {
        return imports.findById(importId).map(entity -> mapper.toDomain(entity, true));
    }

    @Override
    @Transactional
    public Optional<StockImport> findByIdForUpdate(Long importId) {
        return imports.findByIdForUpdate(importId).map(entity -> mapper.toDomain(entity, true));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockImport> findRecent(StockImportStatus status, int limit) {
        PageRequest page = PageRequest.of(0, limit);
        List<StockImportJpaEntity> found = status == null
                ? imports.findRecent(page)
                : imports.findRecentByStatus(status, page);
        return found.stream().map(entity -> mapper.toDomain(entity, false)).toList();
    }

    @Override
    @Transactional
    public StockImport save(StockImport stockImport) {
        StockImportJpaEntity entity = imports.findById(stockImport.id())
                .orElseThrow(() -> new IllegalStateException("Stock import " + stockImport.id() + " not found"));
        mapper.applyHeader(stockImport, entity);
        syncItems(entity, stockImport.items());
        return mapper.toDomain(imports.saveAndFlush(entity), true);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<byte[]> findContent(Long importId) {
        return files.findById(importId).map(StockImportFileJpaEntity::getContent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findRunnable(Instant now, int limit) {
        return imports.findRunnableIds(StockImportStatus.PROCESSING, now, PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public Optional<StockImport> tryAcquire(Long importId, String owner, Instant now, Instant leaseUntil) {
        if (imports.acquire(importId, owner, now, leaseUntil, StockImportStatus.PROCESSING) == 0) {
            return Optional.empty();
        }
        return imports.findById(importId).map(entity -> mapper.toDomain(entity, false));
    }

    @Override
    @Transactional
    public void updateStage(Long importId, String owner, StockImportStage stage, Instant now) {
        imports.updateStage(importId, owner, stage, now, StockImportStatus.PROCESSING);
    }

    @Override
    @Transactional
    public boolean complete(StockImport extracted, String owner, Instant now) {
        Optional<StockImportJpaEntity> locked = imports.findByIdForUpdate(extracted.id());
        if (locked.isEmpty()) {
            return false;
        }
        StockImportJpaEntity entity = locked.get();
        if (entity.getStatus() != StockImportStatus.PROCESSING || !owner.equals(entity.getLeaseOwner())) {
            return false;
        }
        mapper.applyHeader(extracted.toBuilder().updatedAt(now).build(), entity);
        entity.setLeaseOwner(null);
        entity.setLeaseUntil(null);
        entity.setFinishedAt(now);
        entity.getItems().clear();
        syncItems(entity, extracted.items());
        imports.saveAndFlush(entity);
        return true;
    }

    @Override
    @Transactional
    public boolean fail(Long importId, String owner, StockImportFailureReason reason, String message, Instant now) {
        String clipped = message == null ? null : String.format(MAX_MESSAGE, message);
        return imports.fail(importId, owner, reason, clipped, now, StockImportStatus.FAILED,
                StockImportStatus.PROCESSING) == 1;
    }

    @Override
    @Transactional
    public boolean requeue(Long importId, Instant now) {
        return imports.requeue(importId, now, StockImportStatus.PROCESSING, StockImportStage.RECEIVED,
                StockImportStatus.FAILED) == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findConfirmedDuplicate(Long importId, String accessKey, String fileSha256) {
        return imports.findConfirmedDuplicates(importId, accessKey, fileSha256, StockImportStatus.CONFIRMED,
                PageRequest.of(0, 1)).stream().findFirst();
    }

    @Override
    @Transactional
    public int purgeFiles(Instant finishedBefore) {
        return files.purge(List.of(StockImportStatus.CONFIRMED, StockImportStatus.CANCELED), finishedBefore);
    }

    private void syncItems(StockImportJpaEntity entity, List<StockImportItem> items) {
        Map<Long, StockImportItemJpaEntity> existing = entity.getItems().stream()
                .collect(Collectors.toMap(StockImportItemJpaEntity::getId, Function.identity()));
        Set<Long> kept = new HashSet<>();
        List<StockImportItemJpaEntity> added = new ArrayList<>();
        for (StockImportItem item : items) {
            StockImportItemJpaEntity target = item.id() == null ? null : existing.get(item.id());
            if (target == null) {
                target = new StockImportItemJpaEntity();
                target.setStockImport(entity);
                added.add(target);
            } else {
                kept.add(item.id());
            }
            mapper.applyItem(item, target);
        }
        entity.getItems().removeIf(item -> !kept.contains(item.getId()));
        entity.getItems().addAll(added);
        entity.setItemCount(entity.getItems().size());
    }
}
