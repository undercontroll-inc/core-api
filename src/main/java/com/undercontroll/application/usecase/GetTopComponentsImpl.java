package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.GetTopComponentsPort;
import com.undercontroll.application.dto.TopComponentsResponse;
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
