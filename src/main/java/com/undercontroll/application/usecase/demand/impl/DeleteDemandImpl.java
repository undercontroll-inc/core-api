package com.undercontroll.application.usecase.demand.impl;

import com.undercontroll.application.usecase.demand.DeleteDemandPort;
import com.undercontroll.domain.repository.DemandRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteDemandImpl implements DeleteDemandPort {

    private final DemandRepositoryPort demandRepositoryPort;

    @Override
    public Output execute(Input input) {
        try {
            demandRepositoryPort.deleteById(input.demandId());
            return new Output(true, "Demand deleted successfully");
        } catch (Exception e) {
            return new Output(false, "Failed to delete demand: " + e.getMessage());
        }
    }
}
