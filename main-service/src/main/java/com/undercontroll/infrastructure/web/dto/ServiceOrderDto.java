package com.undercontroll.infrastructure.web.dto;

import com.undercontroll.domain.entity.ComponentPart;
import com.undercontroll.domain.entity.Order;
import com.undercontroll.domain.entity.User;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

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
