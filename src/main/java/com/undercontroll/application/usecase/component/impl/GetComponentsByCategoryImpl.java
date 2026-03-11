package com.undercontroll.application.usecase.component.impl;

import com.undercontroll.application.usecase.component.GetComponentsByCategoryPort;
import com.undercontroll.domain.repository.ComponentRepositoryPort;
import com.undercontroll.application.dto.ComponentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetComponentsByCategoryImpl implements GetComponentsByCategoryPort {

    private final ComponentRepositoryPort componentRepositoryPort;

    @Override
    public Output execute(Input input) {
        List<ComponentDto> components = componentRepositoryPort
                .findByCategory(input.category())
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
