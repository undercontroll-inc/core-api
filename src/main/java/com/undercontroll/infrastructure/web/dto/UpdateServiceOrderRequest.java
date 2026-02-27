package com.undercontroll.infrastructure.web.dto;

import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.User;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Date;
import java.util.List;

public record UpdateServiceOrderRequest(

        @Id
        @NotNull
        @Positive
        Integer serviceOrderId,

        User user,
        List<ComponentPart> componentPartList,
        Order order,
        boolean fabricGuarantee,
        Integer budget,
        boolean returnGuarantee,
        String description,
        String nf,
        Date date,
        String store,
        String issue

) {
}
