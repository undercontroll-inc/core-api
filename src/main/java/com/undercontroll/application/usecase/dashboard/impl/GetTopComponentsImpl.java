package com.undercontroll.application.usecase.dashboard.impl;

import com.undercontroll.application.dto.TopComponentsResponse;
import com.undercontroll.application.usecase.dashboard.GetTopComponentsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetTopComponentsImpl implements GetTopComponentsPort {

    @Override
    public Output execute(Input input) {
        log.info("Getting top components for period {} and status {}", input.period(), input.status());
        return new Output(new TopComponentsResponse(Collections.emptyList()));
    }
}
