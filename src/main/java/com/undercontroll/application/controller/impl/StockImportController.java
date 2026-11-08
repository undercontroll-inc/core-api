package com.undercontroll.application.controller.impl;

import com.undercontroll.application.controller.StockImportApi;
import com.undercontroll.application.dto.stockimport.StockImportDto;
import com.undercontroll.application.dto.stockimport.StockImportReviewRequest;
import com.undercontroll.application.dto.stockimport.StockImportSummaryDto;
import com.undercontroll.application.dto.storage.UploadReference;
import com.undercontroll.application.dto.storage.UploadRequest;
import com.undercontroll.application.dto.storage.UploadTicketDto;
import com.undercontroll.application.mapper.StockImportDtoMapper;
import com.undercontroll.application.mapper.UploadDtoMapper;
import com.undercontroll.domain.usecase.stockimport.RequestStockImportUploadPort;
import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.domain.usecase.stockimport.CancelStockImportPort;
import com.undercontroll.domain.usecase.stockimport.ConfirmStockImportPort;
import com.undercontroll.domain.usecase.stockimport.CreateStockImportPort;
import com.undercontroll.domain.usecase.stockimport.GetStockImportPort;
import com.undercontroll.domain.usecase.stockimport.ListStockImportsPort;
import com.undercontroll.domain.usecase.stockimport.RetryStockImportPort;
import com.undercontroll.domain.usecase.stockimport.UpdateStockImportPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StockImportController implements StockImportApi {

    private final CreateStockImportPort createStockImportPort;
    private final ListStockImportsPort listStockImportsPort;
    private final GetStockImportPort getStockImportPort;
    private final UpdateStockImportPort updateStockImportPort;
    private final ConfirmStockImportPort confirmStockImportPort;
    private final RetryStockImportPort retryStockImportPort;
    private final CancelStockImportPort cancelStockImportPort;
    private final StockImportDtoMapper mapper;
    private final RequestStockImportUploadPort requestStockImportUploadPort;
    private final UploadDtoMapper uploadMapper;

    @Override
    public ResponseEntity<UploadTicketDto> requestUpload(UploadRequest request) {
        UploadRequest body = request == null ? new UploadRequest(null, null, null) : request;
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadMapper.toDto(requestStockImportUploadPort.execute(
                new RequestStockImportUploadPort.Input(body.contentType(), body.sizeBytes(), body.fileName()))));
    }

    @Override
    public ResponseEntity<StockImportDto> create(UploadReference request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(mapper.toDto(createStockImportPort.execute(request == null ? null : request.uploadId())));
    }

    @Override
    public ResponseEntity<List<StockImportSummaryDto>> list(StockImportStatus status, Integer limit) {
        return ResponseEntity.ok(listStockImportsPort.execute(status, limit).stream().map(mapper::toSummary).toList());
    }

    @Override
    public ResponseEntity<StockImportDto> get(Long importId) {
        return ResponseEntity.ok(mapper.toDto(getStockImportPort.execute(importId)));
    }

    @Override
    public ResponseEntity<StockImportDto> update(Long importId, StockImportReviewRequest request) {
        return ResponseEntity.ok(mapper.toDto(updateStockImportPort.execute(importId, mapper.toReview(request))));
    }

    @Override
    public ResponseEntity<StockImportDto> confirm(Long importId, StockImportReviewRequest request) {
        return ResponseEntity.ok(mapper.toDto(confirmStockImportPort.execute(importId, mapper.toReview(request))));
    }

    @Override
    public ResponseEntity<StockImportDto> retry(Long importId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toDto(retryStockImportPort.execute(importId)));
    }

    @Override
    public ResponseEntity<Void> cancel(Long importId) {
        cancelStockImportPort.execute(importId);
        return ResponseEntity.noContent().build();
    }
}
