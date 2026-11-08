package com.undercontroll.application.dto.order;

import com.undercontroll.application.dto.orderitem.OrderItemDto;
import com.undercontroll.application.dto.user.UserDto;
import com.undercontroll.domain.enums.ApprovalDecision;
import com.undercontroll.domain.enums.OrderActor;
import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record OrderEnrichedDto(
        Integer id,
        UserDto user,
        List<OrderItemDto> appliances,
        List<OrderPartDto> parts,
        Double partsTotal,
        Double laborTotal,
        Double discount,
        Double totalValue,
        String receivedAt,
        @Schema(description = "yyyy-MM-dd") String deadline,
        String nf,
        boolean haveReturnGuarantee,
        String customerDescription,
        String technicalDescription,
        OrderStatus status,
        String updatedAt,
        OrderOutcome outcome,
        @Schema(description = "Public page for the customer; null until a link or budget is sent") String trackingUrl,
        Instant budgetSentAt,
        Double budgetTotal,
        ApprovalDecision approvalDecision,
        Instant decidedAt,
        OrderActor decidedVia,
        Instant completedAt,
        Instant deliveredAt,
        @Schema(description = "Only on single-order responses; null in lists") List<OrderStatusEventDto> statusHistory,
        @Schema(description = "Only on single-order responses; null in lists") List<OrderPhotoDto> photos
) {
}
