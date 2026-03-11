package com.undercontroll.application.usecase.demand;

import com.undercontroll.application.dto.DemandDto;

import java.util.List;

public interface GetDemandsPort {
    record Input(
            Integer orderId
    ) {}

    record Output(
            List<DemandDto> demands
    ) {}

    Output execute(Input input);
}
