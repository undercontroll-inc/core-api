package com.undercontroll.infrastructure.persistence.adapter;

import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.port.out.DemandRepositoryPort;
import com.undercontroll.infrastructure.persistence.entity.DemandJpaEntity;
import com.undercontroll.infrastructure.persistence.entity.OrderJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.DemandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DemandRepositoryAdapter implements DemandRepositoryPort {

    private final DemandRepository demandRepository;

    @Override
    public Demand save(Demand demand) {
        DemandJpaEntity jpaEntity = DemandJpaEntity.fromDomain(demand);
        DemandJpaEntity savedEntity = demandRepository.save(jpaEntity);
        return savedEntity.toDomain();
    }

    @Override
    public void deleteById(Integer id) {
        demandRepository.deleteById(id);
    }

    @Override
    public void deleteByOrder(Order order) {
        OrderJpaEntity orderJpaEntity = OrderJpaEntity.fromDomain(order);
        demandRepository.deleteByOrder(orderJpaEntity);
    }

    @Override
    public Optional<Demand> findById(Integer id) {
        return demandRepository.findById(id).map(DemandJpaEntity::toDomain);
    }

    @Override
    public List<Demand> findAll() {
        return demandRepository.findAll().stream()
                .map(DemandJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Demand> findByOrder(Order order) {
        OrderJpaEntity orderJpaEntity = OrderJpaEntity.fromDomain(order);
        return demandRepository.findByOrder(orderJpaEntity).stream()
                .map(DemandJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Demand> findByOrderAndComponentId(Order order, Integer componentId) {
        OrderJpaEntity orderJpaEntity = OrderJpaEntity.fromDomain(order);
        return demandRepository.findByOrderAndComponent_Id(orderJpaEntity, componentId)
                .map(DemandJpaEntity::toDomain);
    }

}
