package com.undercontroll.application.controller.impl;

import com.undercontroll.application.controller.PublicOrderApi;
import com.undercontroll.application.dto.order.PublicOrderDto;
import com.undercontroll.application.mapper.OrderDtoMapper;
import com.undercontroll.domain.usecase.order.GetPublicOrderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PublicOrderRestController implements PublicOrderApi {

    private final GetPublicOrderPort getPublicOrderPort;
    private final OrderDtoMapper mapper;

    @Override
    public ResponseEntity<PublicOrderDto> get(String token) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(mapper.toDto(getPublicOrderPort.execute(token)));
    }
}
