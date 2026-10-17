package com.undercontroll.domain.gateway;

import com.undercontroll.application.dto.order.ExportOrderRequest;

public interface PdfExportGateway {

    byte[] exportOS(ExportOrderRequest request);

}
