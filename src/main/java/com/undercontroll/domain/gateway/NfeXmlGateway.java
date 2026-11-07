package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.stockimport.InvoiceExtraction;

public interface NfeXmlGateway {

    InvoiceExtraction parse(byte[] xml);
}
