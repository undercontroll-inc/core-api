package com.undercontroll.infrastructure.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.enums.StockImportItemWarning;
import com.undercontroll.domain.enums.StockImportWarning;
import com.undercontroll.domain.model.stockimport.ItemResult;
import com.undercontroll.domain.model.stockimport.MatchCandidate;
import com.undercontroll.domain.model.stockimport.NewComponentSpec;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.model.stockimport.StockImportItem;
import com.undercontroll.infrastructure.persistence.entity.StockImportItemJpaEntity;
import com.undercontroll.infrastructure.persistence.entity.StockImportJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StockImportPersistenceMapper {

    private static final TypeReference<List<MatchCandidate>> CANDIDATES = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public StockImport toDomain(StockImportJpaEntity entity, boolean withItems) {
        return StockImport.builder()
                .id(entity.getId())
                .createdBy(entity.getCreatedBy())
                .status(entity.getStatus())
                .stage(entity.getStage())
                .sourceType(entity.getSourceType())
                .fileName(entity.getFileName())
                .mimeType(entity.getMimeType())
                .fileSha256(entity.getFileSha256())
                .storageKey(entity.getStorageKey())
                .failureReason(entity.getFailureReason())
                .failureMessage(entity.getFailureMessage())
                .supplierName(entity.getSupplierName())
                .supplierCnpj(entity.getSupplierCnpj())
                .invoiceNumber(entity.getInvoiceNumber())
                .invoiceSeries(entity.getInvoiceSeries())
                .issuedAt(entity.getIssuedAt())
                .accessKey(entity.getAccessKey())
                .total(entity.getTotal())
                .warnings(enums(entity.getWarnings(), StockImportWarning::valueOf))
                .duplicateOfImportId(entity.getDuplicateOfImportId())
                .itemCount(entity.getItemCount())
                .attemptCount(entity.getAttemptCount())
                .items(withItems ? entity.getItems().stream().map(this::toDomain).toList() : List.of())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .confirmedAt(entity.getConfirmedAt())
                .build();
    }

    public void applyHeader(StockImport domain, StockImportJpaEntity entity) {
        entity.setCreatedBy(domain.createdBy());
        entity.setStatus(domain.status());
        entity.setStage(domain.stage());
        entity.setSourceType(domain.sourceType());
        entity.setFileName(domain.fileName());
        entity.setMimeType(domain.mimeType());
        entity.setFileSha256(domain.fileSha256());
        entity.setStorageKey(domain.storageKey());
        entity.setFailureReason(domain.failureReason());
        entity.setFailureMessage(domain.failureMessage());
        entity.setSupplierName(domain.supplierName());
        entity.setSupplierCnpj(domain.supplierCnpj());
        entity.setInvoiceNumber(domain.invoiceNumber());
        entity.setInvoiceSeries(domain.invoiceSeries());
        entity.setIssuedAt(domain.issuedAt());
        entity.setAccessKey(domain.accessKey());
        entity.setTotal(domain.total());
        entity.setWarnings(join(domain.warnings()));
        entity.setDuplicateOfImportId(domain.duplicateOfImportId());
        entity.setItemCount(domain.items().size());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        entity.setConfirmedAt(domain.confirmedAt());
    }

    public void applyItem(StockImportItem domain, StockImportItemJpaEntity entity) {
        entity.setLineNumber(domain.lineNumber());
        entity.setSupplierCode(domain.supplierCode());
        entity.setDescription(domain.description());
        entity.setQuantity(domain.quantity());
        entity.setUnit(domain.unit());
        entity.setUnitPrice(domain.unitPrice());
        entity.setTotal(domain.total());
        entity.setConfidence(domain.confidence());
        entity.setWarnings(join(domain.warnings()));
        entity.setMatchType(domain.matchType());
        entity.setMatchedComponentId(domain.matchedComponentId());
        entity.setMatchConfidence(domain.matchConfidence());
        entity.setCandidates(json(domain.candidates()));
        entity.setAction(domain.action());
        entity.setComponentId(domain.componentId());
        entity.setUpdatePrice(domain.updatePrice());
        NewComponentSpec spec = domain.newComponent();
        entity.setNewName(spec == null ? null : spec.name());
        entity.setNewBrand(spec == null ? null : spec.brand());
        entity.setNewCategory(spec == null ? null : spec.category());
        entity.setNewDescription(spec == null ? null : spec.description());
        ItemResult result = domain.result();
        entity.setResultComponentId(result == null ? null : result.componentId());
        entity.setResultCreated(result == null ? null : result.created());
        entity.setResultPreviousQuantity(result == null ? null : result.previousQuantity());
        entity.setResultNewQuantity(result == null ? null : result.newQuantity());
    }

    StockImportItem toDomain(StockImportItemJpaEntity entity) {
        boolean hasSpec = entity.getNewName() != null || entity.getNewBrand() != null
                || entity.getNewCategory() != null || entity.getNewDescription() != null;
        return StockImportItem.builder()
                .id(entity.getId())
                .lineNumber(entity.getLineNumber())
                .supplierCode(entity.getSupplierCode())
                .description(entity.getDescription())
                .quantity(entity.getQuantity())
                .unit(entity.getUnit())
                .unitPrice(entity.getUnitPrice())
                .total(entity.getTotal())
                .confidence(entity.getConfidence())
                .warnings(enums(entity.getWarnings(), StockImportItemWarning::valueOf))
                .matchType(entity.getMatchType())
                .matchedComponentId(entity.getMatchedComponentId())
                .matchConfidence(entity.getMatchConfidence())
                .candidates(candidates(entity.getCandidates()))
                .action(entity.getAction())
                .componentId(entity.getComponentId())
                .updatePrice(entity.isUpdatePrice())
                .newComponent(hasSpec ? new NewComponentSpec(entity.getNewName(), entity.getNewBrand(),
                        entity.getNewCategory(), entity.getNewDescription()) : null)
                .result(entity.getResultComponentId() == null ? null : new ItemResult(
                        entity.getResultComponentId(),
                        Boolean.TRUE.equals(entity.getResultCreated()),
                        entity.getResultPreviousQuantity() == null ? 0L : entity.getResultPreviousQuantity(),
                        entity.getResultNewQuantity() == null ? 0L : entity.getResultNewQuantity()))
                .build();
    }

    private List<MatchCandidate> candidates(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, CANDIDATES);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private String json(List<MatchCandidate> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(candidates);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize match candidates", ex);
        }
    }

    private static <E extends Enum<E>> String join(List<E> values) {
        if (values.isEmpty()) {
            return null;
        }
        return values.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    private static <E extends Enum<E>> List<E> enums(String value, Function<String, E> parse) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::strip).map(parse).toList();
    }
}
