package com.undercontroll.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.List;

public record RevenueEvolutionResponse(
    List<DataPoint> dataPoints
) {
    public record DataPoint(
        LocalDate date,
        Double revenue,
        Double profit,
        Long orderCount
    ) {}
}

