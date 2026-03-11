package com.undercontroll.application.usecase.dashboard.impl;

import com.undercontroll.application.dto.DashboardMetricsResponse;
import com.undercontroll.application.usecase.dashboard.GetAverageRepairTimePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetAverageRepairTimeImpl implements GetAverageRepairTimePort {

    @Override
    public Output execute(Input input) {
        log.info("Getting average repair time for period {} and status {}", input.period(), input.status());
        return new Output(new DashboardMetricsResponse(null));
    }
}
