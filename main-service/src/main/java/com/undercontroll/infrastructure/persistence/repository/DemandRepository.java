package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.domain.entity.Demand;
import com.undercontroll.domain.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DemandRepository extends JpaRepository<Demand, Integer> {
    List<Demand> findByOrder(Order order);
    Optional<Demand> findByOrderAndComponent_Id(Order order, Integer componentId);
    void deleteByOrder(Order order);
}
