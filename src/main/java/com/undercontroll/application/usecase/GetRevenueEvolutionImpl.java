package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.GetRevenueEvolutionPort;
import com.undercontroll.application.dto.RevenueEvolutionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetRevenueEvolutionImpl implements GetRevenueEvolutionPort {

    @Override
    public Output execute(Input input) {
        log.info("Getting revenue evolution for period {} and status {}", input.period(), input.status());
        return new Output(new RevenueEvolutionResponse(Collections.emptyList()));
    }
}
