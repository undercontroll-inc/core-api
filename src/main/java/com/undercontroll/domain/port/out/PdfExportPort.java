package com.undercontroll.domain.port.out;

import com.undercontroll.application.dto.ExportOrderRequest;

public interface PdfExportPort {

    byte[] exportOS(ExportOrderRequest request);

}
