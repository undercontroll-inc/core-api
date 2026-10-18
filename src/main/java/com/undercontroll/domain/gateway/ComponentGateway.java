package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.ComponentPart;

import java.util.List;
import java.util.Optional;

public interface ComponentGateway {

    ComponentPart save(ComponentPart component);

    void deleteById(Integer id);

    Optional<ComponentPart> findById(Integer id);

    List<ComponentPart> findAll(int limit);

    List<ComponentPart> findLowStock(long maxQuantity, int limit);

    List<ComponentPart> findLowestStock(int limit);

    List<ComponentPart> findByName(String name, int limit);

    List<ComponentPart> searchByName(String name, int limit);

    List<ComponentPart> findByCategory(String category, int limit);

    List<ComponentPart> searchByCategory(String category, int limit);

}
