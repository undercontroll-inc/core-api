package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.ServiceOrderDocument;

public interface PdfExportGateway {

    byte[] exportOS(ServiceOrderDocument document);

}
