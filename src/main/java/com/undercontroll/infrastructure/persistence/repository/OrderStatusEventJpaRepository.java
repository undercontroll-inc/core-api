package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.infrastructure.persistence.entity.OrderStatusEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusEventJpaRepository extends JpaRepository<OrderStatusEventJpaEntity, Long> {

    List<OrderStatusEventJpaEntity> findByOrderIdOrderByCreatedAtAscIdAsc(Integer orderId);
}
