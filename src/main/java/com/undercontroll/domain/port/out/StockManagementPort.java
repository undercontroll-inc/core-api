package com.undercontroll.domain.port.out;

import com.undercontroll.domain.model.ComponentPart;

import java.util.Optional;

public interface StockManagementPort {

    Optional<ComponentPart> findComponentById(Integer componentId);

    ComponentPart save(ComponentPart component);

    void decreaseStock(Integer componentId, Integer quantity);

    void increaseStock(Integer componentId, Integer quantity);

    void validateStockAvailability(ComponentPart component, Integer requiredQuantity);

}
