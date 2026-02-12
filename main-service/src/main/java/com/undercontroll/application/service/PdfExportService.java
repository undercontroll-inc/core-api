package com.undercontroll.application.service;

import com.undercontroll.infrastructure.web.dto.ExportOrderRequest;

public interface PdfExportService {

    byte[] exportOS(ExportOrderRequest request);

}
