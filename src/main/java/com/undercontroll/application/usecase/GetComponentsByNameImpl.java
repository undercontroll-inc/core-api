package com.undercontroll.application.usecase;

import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.port.in.GetComponentsByNamePort;
import com.undercontroll.domain.port.out.ComponentRepositoryPort;
import com.undercontroll.application.dto.ComponentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetComponentsByNameImpl implements GetComponentsByNamePort {

    private final ComponentRepositoryPort componentRepositoryPort;

    @Override
    public Output execute(Input input) {
        List<ComponentDto> components = componentRepositoryPort
                .findByName(input.name())
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
