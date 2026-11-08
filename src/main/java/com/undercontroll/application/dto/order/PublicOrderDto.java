package com.undercontroll.application.dto.order;

import com.undercontroll.domain.enums.ApprovalDecision;
import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PublicOrderDto(
        Integer orderNumber,
        String customerFirstName,
        Device device,
        OrderStatus status,
        String statusLabel,
        OrderOutcome outcome,
        List<TimelineEntry> timeline,
        String devicePhotoUrl,
        Budget budget,
        ApprovalDecision approvalDecision,
        Instant decidedAt,
        boolean canDecide,
        Shop shop
) {

    public record Device(String type, String brand, String model) {
    }

    public record TimelineEntry(String label, LocalDate date) {
    }

    public record Budget(Double laborTotal, Double partsTotal, Double discount, Double total, String warranty) {
    }

    public record Shop(String name, String phone, String whatsappUrl, String address, String hours) {
    }
}
