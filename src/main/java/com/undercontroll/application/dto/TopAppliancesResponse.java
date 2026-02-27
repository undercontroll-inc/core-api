package com.undercontroll.application.dto;

import java.util.List;

public record TopAppliancesResponse(
        List<ApplianceCount> appliances
) {
    public record ApplianceCount(
            String type,
            String brand,
            Long count
    ) {
    }
}
