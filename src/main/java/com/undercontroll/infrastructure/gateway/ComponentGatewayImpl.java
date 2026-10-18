package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.gateway.ComponentGateway;
import com.undercontroll.infrastructure.mapper.ComponentPartMapper;
import com.undercontroll.infrastructure.persistence.entity.ComponentPartJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.ComponentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComponentGatewayImpl implements ComponentGateway {

    private final ComponentJpaRepository componentJpaRepository;
    private final ComponentPartMapper componentPartMapper;

    @Override
    @Transactional
    public ComponentPart save(ComponentPart component) {
        ComponentPartJpaEntity jpaEntity = componentPartMapper.toEntityWithId(component);
        ComponentPartJpaEntity savedEntity = componentJpaRepository.save(jpaEntity);
        return componentPartMapper.toDomain(savedEntity);
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        componentJpaRepository.deleteById(id);
    }

    @Override
    public Optional<ComponentPart> findById(Integer id) {
        return componentJpaRepository.findById(id).map(componentPartMapper::toDomain);
    }

    @Override
    public List<ComponentPart> findAll(int limit) {
        return componentJpaRepository.findSlice(firstPage(limit)).stream()
                .map(componentPartMapper::toDomain)
                .toList();
    }

    @Override
    public List<ComponentPart> findLowStock(long maxQuantity, int limit) {
        return componentJpaRepository.findLowStock(maxQuantity, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(componentPartMapper::toDomain)
                .toList();
    }

    @Override
    public List<ComponentPart> findLowestStock(int limit) {
        return componentJpaRepository.findLowestStock(PageRequest.of(0, Math.max(1, limit))).stream()
                .map(componentPartMapper::toDomain)
                .toList();
    }

    @Override
    public List<ComponentPart> findByName(String name, int limit) {
        return componentJpaRepository.findByName(name, firstPage(limit)).stream()
                .map(componentPartMapper::toDomain)
                .toList();
    }

    @Override
    public List<ComponentPart> searchByName(String name, int limit) {
        String term = sanitizeLike(name);
        if (term.isBlank()) {
            return List.of();
        }
        return componentJpaRepository.searchByName(term, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(componentPartMapper::toDomain)
                .toList();
    }

    @Override
    public List<ComponentPart> findByCategory(String category, int limit) {
        return componentJpaRepository.findByCategory(category, firstPage(limit)).stream()
                .map(componentPartMapper::toDomain)
                .toList();
    }

    @Override
    public List<ComponentPart> searchByCategory(String category, int limit) {
        String term = sanitizeLike(category);
        if (term.isBlank()) {
            return List.of();
        }
        return componentJpaRepository.searchByCategory(term, PageRequest.of(0, Math.max(1, limit))).stream()
                .map(componentPartMapper::toDomain)
                .toList();
    }

    private static PageRequest firstPage(int limit) {
        return PageRequest.of(0, Math.max(1, limit), Sort.by(Sort.Direction.ASC, "id"));
    }

    private static String sanitizeLike(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("%", "").replace("_", "").strip();
    }

}
