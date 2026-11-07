package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.infrastructure.persistence.entity.ComponentSupplierAliasJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComponentSupplierAliasJpaRepository extends JpaRepository<ComponentSupplierAliasJpaEntity, Long> {

    Optional<ComponentSupplierAliasJpaEntity> findBySupplierKeyAndProductKey(String supplierKey, String productKey);
}
