package com.undercontroll.domain.usecase.order;

import com.undercontroll.domain.enums.ApprovalDecision;
import com.undercontroll.domain.enums.OrderActor;
import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderStatusEvent;
import com.undercontroll.domain.model.ShopProfile;
import com.undercontroll.domain.usecase.order_item.OrderItemOutput;
import com.undercontroll.domain.usecase.user.UserOutput;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record OrderOutput(
        Integer id,
        UserOutput user,
        List<OrderItemOutput> appliances,
        List<OrderPartOutput> parts,
        Double partsTotal,
        Double laborTotal,
        Double discount,
        Double totalValue,
        String receivedAt,
        String deadline,
        String nf,
        boolean haveReturnGuarantee,
        String customerDescription,
        String technicalDescription,
        OrderStatus status,
        String updatedAt,
        OrderOutcome outcome,
        String trackingUrl,
        Instant budgetSentAt,
        Double budgetTotal,
        ApprovalDecision approvalDecision,
        Instant decidedAt,
        OrderActor decidedVia,
        Instant completedAt,
        Instant deliveredAt,
        List<OrderStatusEvent> statusHistory,
        List<OrderPhotoOutput> photos
) {

    public static OrderOutput from(Order order, boolean includeTechnical, ShopProfile shop) {
        return new OrderOutput(
                order.getId(),
                UserOutput.from(order.getUser()),
                new ArrayList<>(order.getOrderItems().stream().map(OrderItemOutput::from).toList()),
                new ArrayList<>(order.getDemands().stream().map(OrderPartOutput::from).toList()),
                order.calculatePartsTotal(),
                order.calculateLaborTotal(),
                order.getDiscount(),
                order.getTotal(),
                order.getReceived_at() != null ? order.getReceived_at().toString() : null,
                order.getDeadline() != null ? order.getDeadline().toString() : null,
                order.getNf(),
                order.isReturnGuarantee(),
                order.getCustomerDescription(),
                includeTechnical ? order.getTechnicalDescription() : null,
                order.getStatus(),
                order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null,
                order.getOutcome(),
                order.getTrackingToken() != null && shop != null ? shop.trackingUrl(order.getTrackingToken()) : null,
                order.getBudgetSentAt(),
                order.getBudgetTotal(),
                order.getApprovalDecision(),
                order.getDecidedAt(),
                order.getDecidedVia(),
                order.getCompletedAt(),
                order.getDeliveredAt(),
                null,
                null
        );
    }

    public OrderOutput withDetails(List<OrderStatusEvent> history, List<OrderPhotoOutput> photoOutputs) {
        return new OrderOutput(id, user, appliances, parts, partsTotal, laborTotal, discount, totalValue, receivedAt,
                deadline, nf, haveReturnGuarantee, customerDescription, technicalDescription, status, updatedAt,
                outcome, trackingUrl, budgetSentAt, budgetTotal, approvalDecision, decidedAt, decidedVia,
                completedAt, deliveredAt, new ArrayList<>(history), new ArrayList<>(photoOutputs));
    }
}
