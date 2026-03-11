package com.undercontroll.application.usecase.dashboard.impl;

import com.undercontroll.application.dto.CustomerTypeResponse;
import com.undercontroll.application.usecase.dashboard.GetCustomerTypeEvolutionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetCustomerTypeEvolutionImpl implements GetCustomerTypeEvolutionPort {

    @Override
    public Output execute(Input input) {
        log.info("Getting customer type evolution for period {} and status {}", input.period(), input.status());
        return new Output(new CustomerTypeResponse(Collections.emptyList()));
    }
}
