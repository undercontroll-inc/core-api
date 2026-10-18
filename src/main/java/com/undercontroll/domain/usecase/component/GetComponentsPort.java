package com.undercontroll.domain.usecase.component;

import java.util.List;

public interface GetComponentsPort {
    List<ComponentOutput> execute(String category, String name);
}
