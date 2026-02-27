package com.undercontroll.infrastructure.web.dto;

import java.time.LocalDateTime;
import java.util.Date;

public record CreateServiceOrderRequest(

        Integer orderId,
        boolean haveFabricGuarantee,
        Double budget,
        boolean haveReturnGuarantee,
        String description,
        String nf,
        Date date,
        String store,
//        String issue,
//        LocalDateTime withdrawalAt,
        LocalDateTime received_at

) {
}
