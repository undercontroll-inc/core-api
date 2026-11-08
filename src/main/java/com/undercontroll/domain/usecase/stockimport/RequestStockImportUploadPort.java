package com.undercontroll.domain.usecase.stockimport;

import com.undercontroll.domain.storage.UploadTicket;

public interface RequestStockImportUploadPort {

    UploadTicket execute(Input input);

    record Input(String contentType, Long sizeBytes, String fileName) {
    }
}
