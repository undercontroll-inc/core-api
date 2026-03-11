package com.undercontroll.infrastructure.persistence.repository.jpa;

import com.undercontroll.infrastructure.persistence.entity.DemandJpaEntity;
import com.undercontroll.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DemandRepository extends JpaRepository<DemandJpaEntity, Integer> {
    List<DemandJpaEntity> findByOrder(OrderJpaEntity order);
    Optional<DemandJpaEntity> findByOrderAndComponent_Id(OrderJpaEntity order, Integer componentId);
    void deleteByOrder(OrderJpaEntity order);
}
