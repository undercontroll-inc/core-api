package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.domain.enums.OrderPhotoKind;
import com.undercontroll.infrastructure.persistence.entity.OrderPhotoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderPhotoJpaRepository extends JpaRepository<OrderPhotoJpaEntity, Long> {

    List<OrderPhotoJpaEntity> findByOrderIdOrderByCreatedAtAscIdAsc(Integer orderId);

    Optional<OrderPhotoJpaEntity> findFirstByOrderIdAndKindOrderByCreatedAtAscIdAsc(Integer orderId, OrderPhotoKind kind);

    Optional<OrderPhotoJpaEntity> findByIdAndOrderId(Long id, Integer orderId);

    long countByOrderId(Integer orderId);

    Optional<OrderPhotoJpaEntity> findByStorageKey(String storageKey);
}
