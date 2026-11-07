package com.undercontroll.domain.model.stockimport;

public record MatchCandidate(
        Integer componentId,
        String name,
        String brand,
        Long currentQuantity,
        Double price,
        double score
) {
}
