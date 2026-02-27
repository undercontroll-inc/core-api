package com.undercontroll.application.usecase;

import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.port.in.GetComponentByIdPort;
import com.undercontroll.domain.port.out.ComponentRepositoryPort;
import com.undercontroll.application.dto.ComponentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetComponentByIdImpl implements GetComponentByIdPort {

    private final ComponentRepositoryPort componentRepositoryPort;

    @Override
    public Output execute(Input input) {
        Optional<ComponentPart> component = componentRepositoryPort.findById(input.componentId());
        if (component.isEmpty()) {
            return new Output(null);
        }
        ComponentPart c = component.get();
        return new Output(new ComponentDto(
                c.getId(),
                c.getName(),
                c.getDescription(),
                c.getBrand(),
                c.getPrice(),
                c.getQuantity(),
                c.getSupplier(),
                c.getCategory()
        ));
    }
}
