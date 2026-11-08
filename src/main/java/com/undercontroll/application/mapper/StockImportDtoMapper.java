package com.undercontroll.application.mapper;

import com.undercontroll.application.dto.stockimport.StockImportDto;
import com.undercontroll.application.dto.stockimport.StockImportItemDto;
import com.undercontroll.application.dto.stockimport.StockImportReviewRequest;
import com.undercontroll.application.dto.stockimport.StockImportSummaryDto;
import com.undercontroll.domain.model.stockimport.MatchCandidate;
import com.undercontroll.domain.model.stockimport.NewComponentSpec;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.model.stockimport.StockImportItem;
import com.undercontroll.domain.model.stockimport.StockImportReview;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StockImportDtoMapper {

    public StockImportReview toReview(StockImportReviewRequest request) {
        if (request == null) {
            return null;
        }
        StockImportDto.Supplier supplier = request.supplier();
        StockImportReviewRequest.Invoice invoice = request.invoice();
        List<StockImportReview.Item> items = request.items() == null ? null : request.items().stream()
                .map(item -> item == null ? null : new StockImportReview.Item(
                        item.id(),
                        item.description(),
                        item.quantity(),
                        item.unit(),
                        item.unitPrice(),
                        item.action(),
                        item.componentId(),
                        Boolean.TRUE.equals(item.updatePrice()),
                        item.newComponent() == null ? null : new NewComponentSpec(
                                item.newComponent().name(),
                                item.newComponent().brand(),
                                item.newComponent().category(),
                                item.newComponent().description())))
                .toList();
        return new StockImportReview(
                supplier == null ? null : supplier.name(),
                supplier == null ? null : supplier.cnpj(),
                invoice == null ? null : invoice.number(),
                invoice == null ? null : invoice.series(),
                invoice == null ? null : invoice.issuedAt(),
                items
        );
    }

    public StockImportDto toDto(StockImport stockImport) {
        return new StockImportDto(
                stockImport.id(),
                stockImport.status(),
                stockImport.stage(),
                stockImport.sourceType(),
                stockImport.fileName(),
                stockImport.failureReason(),
                stockImport.failureMessage(),
                new StockImportDto.Supplier(stockImport.supplierName(), stockImport.supplierCnpj()),
                new StockImportDto.Invoice(stockImport.invoiceNumber(), stockImport.invoiceSeries(),
                        stockImport.issuedAt(), stockImport.accessKey(), stockImport.total()),
                stockImport.itemsTotal(),
                stockImport.warnings(),
                stockImport.duplicateOfImportId(),
                stockImport.items().stream().map(this::toDto).toList(),
                stockImport.createdAt(),
                stockImport.updatedAt(),
                stockImport.confirmedAt()
        );
    }

    public StockImportSummaryDto toSummary(StockImport stockImport) {
        return new StockImportSummaryDto(
                stockImport.id(),
                stockImport.status(),
                stockImport.stage(),
                stockImport.sourceType(),
                stockImport.supplierName(),
                stockImport.invoiceNumber(),
                stockImport.issuedAt(),
                stockImport.total(),
                stockImport.itemCount(),
                stockImport.createdAt()
        );
    }

    StockImportItemDto toDto(StockImportItem item) {
        NewComponentSpec spec = item.newComponent();
        return new StockImportItemDto(
                item.id(),
                item.lineNumber(),
                item.supplierCode(),
                item.description(),
                item.quantity(),
                item.unit(),
                item.unitPrice(),
                item.total(),
                item.confidence(),
                item.warnings(),
                new StockImportItemDto.Match(
                        item.matchType(),
                        item.matchedComponentId(),
                        item.matchConfidence(),
                        item.candidates().stream().map(this::toDto).toList()),
                item.action(),
                item.componentId(),
                item.updatePrice(),
                spec == null ? null : new StockImportItemDto.NewComponent(spec.name(), spec.brand(), spec.category(), spec.description()),
                item.result() == null ? null : new StockImportItemDto.Result(item.result().componentId(),
                        item.result().created(), item.result().previousQuantity(), item.result().newQuantity())
        );
    }

    private StockImportItemDto.Candidate toDto(MatchCandidate candidate) {
        return new StockImportItemDto.Candidate(candidate.componentId(), candidate.name(), candidate.brand(),
                candidate.currentQuantity(), candidate.price(), candidate.score());
    }
}
