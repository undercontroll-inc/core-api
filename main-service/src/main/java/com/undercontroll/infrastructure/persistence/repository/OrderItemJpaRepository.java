package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.domain.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemJpaRepository extends JpaRepository<OrderItem, Integer> {
}
