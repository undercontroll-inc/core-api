package com.undercontroll.domain.port.out;

import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.model.Order;

import java.util.List;
import java.util.Optional;

public interface DemandRepositoryPort {

    Demand save(Demand demand);

    void deleteById(Integer id);

    void deleteByOrder(Order order);

    Optional<Demand> findById(Integer id);

    List<Demand> findAll();

    List<Demand> findByOrder(Order order);

    Optional<Demand> findByOrderAndComponentId(Order order, Integer componentId);

}
