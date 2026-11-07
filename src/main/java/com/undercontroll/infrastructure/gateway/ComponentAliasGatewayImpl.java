package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.gateway.ComponentAliasGateway;
import com.undercontroll.infrastructure.persistence.entity.ComponentSupplierAliasJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.ComponentSupplierAliasJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ComponentAliasGatewayImpl implements ComponentAliasGateway {

    private final ComponentSupplierAliasJpaRepository aliases;

    @Override
    @Transactional(readOnly = true)
    public Optional<Integer> find(String supplierKey, String productKey) {
        return aliases.findBySupplierKeyAndProductKey(supplierKey, productKey)
                .map(ComponentSupplierAliasJpaEntity::getComponentId);
    }

    @Override
    @Transactional
    public void upsert(String supplierKey, String productKey, Integer componentId, Instant now) {
        ComponentSupplierAliasJpaEntity alias = aliases.findBySupplierKeyAndProductKey(supplierKey, productKey)
                .orElseGet(() -> ComponentSupplierAliasJpaEntity.builder()
                        .supplierKey(supplierKey)
                        .productKey(productKey)
                        .build());
        alias.setComponentId(componentId);
        alias.setUpdatedAt(now);
        aliases.save(alias);
    }
}
