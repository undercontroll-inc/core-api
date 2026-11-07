package com.undercontroll.domain.usecase.component.impl;

import com.undercontroll.domain.usecase.component.GetComponentsPort;
import com.undercontroll.domain.usecase.component.ComponentOutput;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.gateway.ComponentGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetComponentsImpl implements GetComponentsPort {

    static final int LIST_LIMIT = 500;

    private final ComponentGateway componentGateway;

    @Override
    @Cacheable(value = "components", key = "#category + '-' + #name")
    public List<ComponentOutput> execute(String category, String name) {
        List<ComponentPart> components;

        if (category != null && !category.isBlank()) {
            components = componentGateway.findByCategory(category, LIST_LIMIT);
            if (name != null && !name.isBlank()) {
                String needle = name.toLowerCase();
                components = components.stream()
                        .filter(c -> c.getName() != null && c.getName().toLowerCase().contains(needle))
                        .toList();
            }
        } else if (name != null && !name.isBlank()) {
            components = componentGateway.findByName(name, LIST_LIMIT);
        } else {
            components = componentGateway.findAll(LIST_LIMIT);
        }

        return components.stream()
                .map(ComponentOutput::from)
                .toList();
    }
}
