package com.undercontroll.application.usecase.dashboard.impl;

import com.undercontroll.application.dto.TopAppliancesResponse;
import com.undercontroll.application.usecase.dashboard.GetTopAppliancesPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetTopAppliancesImpl implements GetTopAppliancesPort {

    @Override
    public Output execute(Input input) {
        log.info("Getting top appliances for period {} and status {}", input.period(), input.status());
        return new Output(new TopAppliancesResponse(Collections.emptyList()));
    }
}
