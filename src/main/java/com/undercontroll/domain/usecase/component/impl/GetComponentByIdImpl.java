package com.undercontroll.domain.usecase.component.impl;

import com.undercontroll.domain.usecase.component.GetComponentByIdPort;
import com.undercontroll.domain.usecase.component.ComponentOutput;
import com.undercontroll.domain.gateway.ComponentGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetComponentByIdImpl implements GetComponentByIdPort {

    private final ComponentGateway componentGateway;

    @Override
    public Optional<ComponentOutput> execute(Integer componentId) {
        return componentGateway.findById(componentId)
                .map(ComponentOutput::from);
    }
}
