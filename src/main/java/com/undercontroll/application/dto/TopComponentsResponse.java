package com.undercontroll.application.dto;

import java.util.List;

public record TopComponentsResponse(
        List<ComponentUsage> components
) {
    public record ComponentUsage(
            Integer componentId,
            String name,
            String brand,
            String category,
            Long totalQuantityUsed
    ) {
    }
}
