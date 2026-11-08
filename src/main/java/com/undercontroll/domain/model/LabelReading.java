package com.undercontroll.domain.model;

import java.util.Set;

public record LabelReading(
        boolean isLabel,
        String type,
        String brand,
        String model,
        String serial,
        String voltage,
        double confidence
) {

    public static final Set<String> VOLTAGES = Set.of("127V", "220V", "BIVOLT");
}
