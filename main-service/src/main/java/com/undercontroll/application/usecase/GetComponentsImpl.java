package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.GetComponentsPort;
import com.undercontroll.infrastructure.persistence.repository.ComponentJpaRepository;
import com.undercontroll.infrastructure.web.dto.ComponentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetComponentsImpl implements GetComponentsPort {

    private final ComponentJpaRepository repository;

    @Override
    @Cacheable(value = "components")
    public Output execute(Input input) {
        List<ComponentDto> components = repository
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
