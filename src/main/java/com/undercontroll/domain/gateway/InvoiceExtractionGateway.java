package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.stockimport.InvoiceExtraction;
import com.undercontroll.domain.model.stockimport.InvoiceFile;

public interface InvoiceExtractionGateway {

    InvoiceExtraction extract(InvoiceFile file);
}
