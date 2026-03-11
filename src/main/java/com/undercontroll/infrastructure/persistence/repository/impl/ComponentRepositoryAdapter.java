package com.undercontroll.infrastructure.persistence.repository.impl;

import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.repository.ComponentRepositoryPort;
import com.undercontroll.infrastructure.persistence.entity.ComponentPartJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.jpa.ComponentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ComponentRepositoryAdapter implements ComponentRepositoryPort {

    private final ComponentJpaRepository componentJpaRepository;

    @Override
    public ComponentPart save(ComponentPart component) {
        ComponentPartJpaEntity jpaEntity = ComponentPartJpaEntity.fromDomain(component);
        ComponentPartJpaEntity savedEntity = componentJpaRepository.save(jpaEntity);
        return savedEntity.toDomain();
    }

    @Override
    public void deleteById(Integer id) {
        componentJpaRepository.deleteById(id);
    }

    @Override
    public Optional<ComponentPart> findById(Integer id) {
        return componentJpaRepository.findById(id).map(ComponentPartJpaEntity::toDomain);
    }

    @Override
    public List<ComponentPart> findAll() {
        return componentJpaRepository.findAll().stream()
                .map(ComponentPartJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<ComponentPart> findByName(String name) {
        return componentJpaRepository.findByName(name).stream()
                .map(ComponentPartJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<ComponentPart> findByCategory(String category) {
        return componentJpaRepository.findByCategory(category).stream()
                .map(ComponentPartJpaEntity::toDomain)
                .toList();
    }

}
