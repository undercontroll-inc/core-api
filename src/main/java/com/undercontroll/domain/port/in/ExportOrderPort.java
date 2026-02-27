package com.undercontroll.domain.port.in;

public interface ExportOrderPort {
    record Input(
            Integer orderId
    ) {}

    record Output(
            byte[] pdfData
    ) {}

    Output execute(Input input);
}
