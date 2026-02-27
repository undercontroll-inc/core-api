package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.GetOrdersByStatusPort;
import com.undercontroll.application.dto.OrdersByStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetOrdersByStatusImpl implements GetOrdersByStatusPort {

    @Override
    public Output execute(Input input) {
        log.info("Getting orders by status for period {}", input.period());
        return new Output(new OrdersByStatusResponse(Collections.emptyList()));
    }
}
