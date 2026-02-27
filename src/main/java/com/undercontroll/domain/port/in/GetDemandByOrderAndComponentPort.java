package com.undercontroll.domain.port.in;

import com.undercontroll.application.dto.DemandDto;

public interface GetDemandByOrderAndComponentPort {
    record Input(
            Integer orderId,
            Integer componentId
    ) {}

    record Output(
            DemandDto demand
    ) {}

    Output execute(Input input);
}
