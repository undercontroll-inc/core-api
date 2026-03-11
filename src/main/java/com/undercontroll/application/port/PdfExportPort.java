package com.undercontroll.application.port;

import com.undercontroll.application.dto.ExportOrderRequest;

public interface PdfExportPort {

    byte[] exportOS(ExportOrderRequest request);

}
