package com.undercontroll.domain.model.dashboard;

import java.time.LocalDate;

public record RevenuePoint(LocalDate date, Double revenue, Double profit, Long orderCount) {
}
