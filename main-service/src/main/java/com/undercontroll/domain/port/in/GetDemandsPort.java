package com.undercontroll.domain.port.in;

import com.undercontroll.infrastructure.web.dto.DemandDto;

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
