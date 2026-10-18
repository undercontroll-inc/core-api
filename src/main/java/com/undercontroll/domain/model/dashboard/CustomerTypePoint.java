package com.undercontroll.domain.model.dashboard;

import java.time.LocalDate;

public record CustomerTypePoint(LocalDate date, Long recurrentCustomers, Long newCustomers) {
}
