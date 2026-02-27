package com.undercontroll.application.dto;

import java.time.LocalDate;
import java.util.List;

public record CustomerTypeResponse(
        List<DataPoint> dataPoints
) {
    public record DataPoint(
            LocalDate date,
            Long recurrentCustomers,
            Long newCustomers
    ) {
    }
}
