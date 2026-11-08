package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.LabelReading;
import com.undercontroll.domain.model.stockimport.InvoiceFile;

public interface LabelExtractionGateway {

    LabelReading extract(InvoiceFile image);
}
