package com.undercontroll.domain.usecase.component.impl;

import com.undercontroll.application.dto.component.ComponentDto;
import com.undercontroll.application.mapper.ComponentPartDtoMapper;
import com.undercontroll.domain.usecase.component.GetComponentsPort;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.gateway.ComponentGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetComponentsImpl implements GetComponentsPort {

    private final ComponentGateway componentGateway;
    private final ComponentPartDtoMapper componentPartDtoMapper;

    @Override
    @Cacheable(value = "components", key = "#category + '-like-' + #name")
    public List<ComponentDto> execute(String category, String name) {
        List<ComponentPart> components;

        if (category != null && !category.isBlank()) {
            components = componentGateway.findByCategory(category);
            if (name != null && !name.isBlank()) {
                String needle = name.toLowerCase();
                components = components.stream()
                        .filter(c -> c.getName() != null && c.getName().toLowerCase().contains(needle))
                        .toList();
            }
        } else if (name != null && !name.isBlank()) {
            components = componentGateway.searchByName(name, 50);
        } else {
            components = componentGateway.findAll();
        }

        return components.stream()
                .map(componentPartDtoMapper::toDto)
                .toList();
    }
}
