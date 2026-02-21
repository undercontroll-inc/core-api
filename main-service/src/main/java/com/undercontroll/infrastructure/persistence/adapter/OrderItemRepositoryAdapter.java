package com.undercontroll.infrastructure.persistence.adapter;

import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.port.out.OrderItemRepositoryPort;
import com.undercontroll.infrastructure.persistence.entity.OrderItemJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.OrderItemJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderItemRepositoryAdapter implements OrderItemRepositoryPort {

    private final OrderItemJpaRepository orderItemJpaRepository;

    @Override
    public OrderItem save(OrderItem orderItem) {
        OrderItemJpaEntity jpaEntity = OrderItemJpaEntity.fromDomain(orderItem);
        OrderItemJpaEntity savedEntity = orderItemJpaRepository.save(jpaEntity);
        return savedEntity.toDomain();
    }

    @Override
    public void deleteById(Integer id) {
        orderItemJpaRepository.deleteById(id);
    }

    @Override
    public Optional<OrderItem> findById(Integer id) {
        return orderItemJpaRepository.findById(id).map(OrderItemJpaEntity::toDomain);
    }

    @Override
    public List<OrderItem> findAll() {
        return orderItemJpaRepository.findAll().stream()
                .map(OrderItemJpaEntity::toDomain)
                .toList();
    }

}
