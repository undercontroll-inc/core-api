package com.undercontroll.application.controller.publicpage;

import com.undercontroll.domain.enums.ApprovalDecision;
import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.order.OrderMessages;
import com.undercontroll.domain.usecase.order.PublicOrderView;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public record PublicOrderPage(
        String token,
        Integer orderNumber,
        String greeting,
        String device,
        String statusLabel,
        String statusTone,
        String devicePhotoUrl,
        boolean canDecide,
        String confirm,
        boolean showBudget,
        String laborTotal,
        String partsTotal,
        String discount,
        String total,
        String warranty,
        String decisionMessage,
        boolean readyForPickup,
        boolean repaired,
        List<Step> timeline,
        PublicOrderView.Shop shop
) {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"));

    public record Step(String label, String date) {
    }

    public static PublicOrderPage of(PublicOrderView view, String confirm, ZoneId zone) {
        PublicOrderView.Budget budget = view.budget();
        boolean awaiting = view.status() == OrderStatus.AWAITING_APPROVAL;
        String confirmation = awaiting && ("APPROVED".equals(confirm) || "DECLINED".equals(confirm)) ? confirm : null;
        return new PublicOrderPage(
                view.token(),
                view.orderNumber(),
                view.customerFirstName() == null ? "Olá!" : "Olá, " + view.customerFirstName() + "!",
                device(view.device()),
                view.statusLabel(),
                tone(view),
                view.devicePhotoUrl(),
                view.canDecide(),
                confirmation,
                budget != null && (awaiting || view.approvalDecision() != null),
                budget == null ? null : OrderMessages.money(budget.laborTotal()),
                budget == null ? null : OrderMessages.money(budget.partsTotal()),
                budget == null || budget.discount() == null || budget.discount() <= 0 ? null : OrderMessages.money(budget.discount()),
                budget == null ? null : OrderMessages.money(budget.total()),
                budget == null ? null : budget.warranty(),
                decisionMessage(view, zone),
                view.status() == OrderStatus.READY_FOR_PICKUP,
                view.outcome() == OrderOutcome.REPAIRED,
                view.timeline().stream()
                        .map(entry -> new Step(entry.label(), entry.date() == null ? "" : DAY.format(entry.date())))
                        .toList(),
                view.shop()
        );
    }

    private static String device(PublicOrderView.Device device) {
        if (device == null) {
            return "Seu aparelho";
        }
        StringBuilder text = new StringBuilder();
        for (String part : new String[]{device.type(), device.brand(), device.model()}) {
            if (part != null && !part.isBlank()) {
                if (!text.isEmpty()) {
                    text.append(' ');
                }
                text.append(part.strip());
            }
        }
        if (text.isEmpty()) {
            return "Seu aparelho";
        }
        return text.substring(0, 1).toUpperCase(Locale.forLanguageTag("pt-BR")) + text.substring(1);
    }

    private static String tone(PublicOrderView view) {
        return switch (view.status()) {
            case AWAITING_APPROVAL -> "attention";
            case READY_FOR_PICKUP -> "ready";
            case DELIVERED -> "done";
            default -> "progress";
        };
    }

    private static String decisionMessage(PublicOrderView view, ZoneId zone) {
        if (view.approvalDecision() == null || view.decidedAt() == null) {
            return null;
        }
        String day = DAY.format(view.decidedAt().atZone(zone));
        return view.approvalDecision() == ApprovalDecision.APPROVED
                ? "Conserto aprovado em " + day + ". Obrigado! Avisaremos quando estiver pronto."
                : "Conserto recusado em " + day + ". Não há cobrança. Pode buscar seu aparelho na loja.";
    }
}
