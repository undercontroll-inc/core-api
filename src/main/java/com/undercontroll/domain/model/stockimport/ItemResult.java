package com.undercontroll.domain.model.stockimport;

public record ItemResult(Integer componentId, boolean created, long previousQuantity, long newQuantity) {
}
