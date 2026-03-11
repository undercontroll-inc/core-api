package com.undercontroll.application.usecase.component.impl;

import com.undercontroll.application.usecase.component.GetComponentsPort;
import com.undercontroll.domain.repository.ComponentRepositoryPort;
import com.undercontroll.application.dto.ComponentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetComponentsImpl implements GetComponentsPort {

    private final ComponentRepositoryPort componentRepositoryPort;

    @Override
    @Cacheable(value = "components")
    public Output execute() {
        List<ComponentDto> components = componentRepositoryPort
                .findAll()
                .stream()
                .map(c -> new ComponentDto(
                        c.getId(),
                        c.getName(),
                        c.getDescription(),
                        c.getBrand(),
                        c.getPrice(),
                        c.getQuantity(),
                        c.getSupplier(),
                        c.getCategory()
                ))
                .toList();

        return new Output(components);
    }
}
