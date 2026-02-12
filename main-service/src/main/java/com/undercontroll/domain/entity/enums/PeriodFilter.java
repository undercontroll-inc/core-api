package com.undercontroll.domain.entity.enums;

public enum PeriodFilter {
    SEVEN_DAYS(7),
    THIRTY_DAYS(30),
    NINETY_DAYS(90),
    YEAR(365),
    ALL(null);

    private final Integer days;

    PeriodFilter(Integer days) {
        this.days = days;
    }

    public Integer getDays() {
        return days;
    }
}

