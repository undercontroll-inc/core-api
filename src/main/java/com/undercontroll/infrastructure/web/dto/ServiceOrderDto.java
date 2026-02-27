package com.undercontroll.infrastructure.web.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Date;

@Builder
public record ServiceOrderDto (

        boolean fabricGuarantee,
        boolean returnGuarantee,
        String description,
        String nf,
        Date date,
        String store,
        String issue,
        LocalDateTime withDrawAt,
        LocalDateTime receivedAt
){
}
