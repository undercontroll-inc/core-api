package com.undercontroll.application.usecase.order;

public interface ExportOrderPort {
    record Input(
            Integer orderId
    ) {}

    record Output(
            byte[] pdfData
    ) {}

    Output execute(Input input);
}
