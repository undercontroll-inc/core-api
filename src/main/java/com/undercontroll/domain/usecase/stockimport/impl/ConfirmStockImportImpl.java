package com.undercontroll.domain.usecase.stockimport.impl;

import com.undercontroll.domain.enums.StockImportItemAction;
import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.domain.exception.FieldViolation;
import com.undercontroll.domain.exception.StockImportComponentNotFoundException;
import com.undercontroll.domain.exception.StockImportInvalidStateException;
import com.undercontroll.domain.exception.StockImportNotFoundException;
import com.undercontroll.domain.gateway.ComponentAliasGateway;
import com.undercontroll.domain.gateway.ComponentGateway;
import com.undercontroll.domain.gateway.StockImportGateway;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.stockimport.ItemResult;
import com.undercontroll.domain.model.stockimport.NewComponentSpec;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.model.stockimport.StockImportItem;
import com.undercontroll.domain.model.stockimport.StockImportReview;
import com.undercontroll.domain.stockimport.StockImportKeys;
import com.undercontroll.domain.stockimport.StockImportReviews;
import com.undercontroll.domain.support.Texts;
import com.undercontroll.domain.usecase.stockimport.ConfirmStockImportPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmStockImportImpl implements ConfirmStockImportPort {

    private final StockImportGateway stockImportGateway;
    private final ComponentGateway componentGateway;
    private final ComponentAliasGateway componentAliasGateway;
    private final Clock clock;

    @Override
    @Transactional
    @CacheEvict(value = {"components", "componentsByCategory", "componentsByName", "component"}, allEntries = true)
    public StockImport execute(Long importId, StockImportReview review) {
        StockImport current = stockImportGateway.findByIdForUpdate(importId)
                .orElseThrow(() -> new StockImportNotFoundException(importId));
        if (current.status() != StockImportStatus.READY) {
            throw new StockImportInvalidStateException(importId, current.status(), "confirmed");
        }
        StockImport reviewed = StockImportReviews.apply(current, review, true);
        requireComponents(review);

        Instant now = clock.instant();
        Optional<String> supplierKey = StockImportKeys.supplierKey(reviewed.supplierCnpj(), reviewed.supplierName());
        List<StockImportItem> items = new ArrayList<>();
        int added = 0;
        int created = 0;
        for (StockImportItem item : reviewed.items()) {
            ItemResult result = switch (item.action()) {
                case ADD_TO_EXISTING -> addToExisting(item);
                case CREATE_NEW -> createNew(item, reviewed.supplierName());
                case IGNORE -> null;
            };
            if (result != null) {
                if (result.created()) {
                    created++;
                } else {
                    added++;
                }
                learnAlias(supplierKey, item, result.componentId(), now);
            }
            items.add(item.toBuilder().result(result).build());
        }
        StockImport confirmed = stockImportGateway.save(reviewed.toBuilder()
                .status(StockImportStatus.CONFIRMED)
                .stage(null)
                .items(items)
                .confirmedAt(now)
                .updatedAt(now)
                .build());
        log.info("Stock import confirmed id={} added={} created={} ignored={}",
                importId, added, created, items.size() - added - created);
        return confirmed;
    }

    private void requireComponents(StockImportReview review) {
        List<FieldViolation> missing = new ArrayList<>();
        for (int i = 0; i < review.items().size(); i++) {
            StockImportReview.Item item = review.items().get(i);
            if (item.action() == StockImportItemAction.ADD_TO_EXISTING
                    && !componentGateway.existsById(item.componentId())) {
                missing.add(new FieldViolation("items[" + i + "].componentId",
                        "component " + item.componentId() + " does not exist"));
            }
        }
        if (!missing.isEmpty()) {
            throw new StockImportComponentNotFoundException(missing);
        }
    }

    private ItemResult addToExisting(StockImportItem item) {
        long quantity = item.quantity().longValueExact();
        Double price = item.updatePrice() && item.unitPrice() != null ? item.unitPrice().doubleValue() : null;
        ComponentPart updated = componentGateway.addStock(item.componentId(), quantity, price)
                .orElseThrow(() -> new StockImportComponentNotFoundException(List.of(new FieldViolation(
                        "componentId", "component " + item.componentId() + " does not exist"))));
        long newQuantity = updated.getQuantity();
        return new ItemResult(updated.getId(), false, newQuantity - quantity, newQuantity);
    }

    private ItemResult createNew(StockImportItem item, String supplierName) {
        long quantity = item.quantity().longValueExact();
        NewComponentSpec spec = item.newComponent();
        String name = spec == null ? null : spec.name();
        ComponentPart saved = componentGateway.save(ComponentPart.builder()
                .name(Texts.clip(Texts.firstNonBlank(name, item.description()), 255))
                .description(Texts.clip(Texts.firstNonBlank(spec == null ? null : spec.description(), item.description()), 255))
                .brand(spec == null ? null : spec.brand())
                .category(spec == null ? null : spec.category())
                .price(item.unitPrice() == null ? null : item.unitPrice().doubleValue())
                .supplier(Texts.clip(supplierName, 255))
                .quantity(quantity)
                .build());
        return new ItemResult(saved.getId(), true, 0L, quantity);
    }

    private void learnAlias(Optional<String> supplierKey, StockImportItem item, Integer componentId, Instant now) {
        if (supplierKey.isEmpty()) {
            return;
        }
        StockImportKeys.productKey(item.supplierCode(), item.description())
                .ifPresent(productKey -> componentAliasGateway.upsert(supplierKey.get(), productKey, componentId, now));
    }
}
