package com.undercontroll.domain.port.out;

import com.undercontroll.infrastructure.web.dto.ExportOrderRequest;

public interface PdfExportPort {

    byte[] exportOS(ExportOrderRequest request);

}
