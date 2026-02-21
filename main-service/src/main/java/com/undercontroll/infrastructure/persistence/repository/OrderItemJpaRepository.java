package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.infrastructure.persistence.entity.OrderItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemJpaRepository extends JpaRepository<OrderItemJpaEntity, Integer> {
}
