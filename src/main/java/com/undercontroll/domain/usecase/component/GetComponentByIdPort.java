package com.undercontroll.domain.usecase.component;

import java.util.Optional;

public interface GetComponentByIdPort {
    Optional<ComponentOutput> execute(Integer componentId);
}
