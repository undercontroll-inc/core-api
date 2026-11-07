package com.undercontroll.domain.usecase.demand;

import java.util.List;

public interface GetDemandsPort {
    List<DemandOutput> execute(Integer orderId, Integer componentId);
}
