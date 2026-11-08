package com.undercontroll.domain.order;

import com.undercontroll.domain.enums.ApprovalDecision;
import com.undercontroll.domain.enums.OrderActor;
import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.exception.InvalidOrderTransitionException;
import com.undercontroll.domain.exception.OrderBudgetEmptyException;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderStatusEvent;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.undercontroll.domain.enums.OrderStatus.AWAITING_APPROVAL;
import static com.undercontroll.domain.enums.OrderStatus.DELIVERED;
import static com.undercontroll.domain.enums.OrderStatus.IN_REPAIR;
import static com.undercontroll.domain.enums.OrderStatus.READY_FOR_PICKUP;
import static com.undercontroll.domain.enums.OrderStatus.RECEIVED;

public final class OrderLifecycle {

    public static final int NOTE_MAX = 500;

    static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            RECEIVED, EnumSet.of(AWAITING_APPROVAL, READY_FOR_PICKUP),
            AWAITING_APPROVAL, EnumSet.of(IN_REPAIR, READY_FOR_PICKUP, RECEIVED),
            IN_REPAIR, EnumSet.of(READY_FOR_PICKUP),
            READY_FOR_PICKUP, EnumSet.of(DELIVERED),
            DELIVERED, EnumSet.noneOf(OrderStatus.class)
    );

    private OrderLifecycle() {
    }

    public static boolean allowed(OrderStatus from, OrderStatus to) {
        return from != null && to != null && ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static OrderStatusEvent transition(
            Order order,
            OrderStatus to,
            OrderOutcome outcome,
            OrderActor actor,
            String note,
            Instant now
    ) {
        OrderStatus from = order.getStatus();
        if (!allowed(from, to)) {
            throw new InvalidOrderTransitionException(
                    "Não é possível mudar o pedido de " + label(from) + " para " + label(to));
        }
        OrderOutcome resolved = resolveOutcome(from, to, outcome);
        switch (to) {
            case AWAITING_APPROVAL -> sendBudget(order, now);
            case IN_REPAIR -> decide(order, ApprovalDecision.APPROVED, actor, now);
            case READY_FOR_PICKUP -> {
                if (from == AWAITING_APPROVAL) {
                    decide(order, ApprovalDecision.DECLINED, actor, now);
                }
                order.setOutcome(resolved);
                order.setCompletedAt(now);
            }
            case RECEIVED -> {
                order.setApprovalDecision(null);
                order.setDecidedAt(null);
                order.setDecidedVia(null);
            }
            case DELIVERED -> order.setDeliveredAt(now);
        }
        order.setStatus(to);
        return new OrderStatusEvent(null, order.getId(), from, to, resolved, actor, clean(note), now);
    }

    public static Optional<OrderStatusEvent> decideBudget(
            Order order,
            ApprovalDecision decision,
            OrderActor actor,
            String note,
            Instant now
    ) {
        if (decision != ApprovalDecision.APPROVED && decision != ApprovalDecision.DECLINED) {
            throw new InvalidOrderTransitionException("A decisão deve ser APPROVED ou DECLINED");
        }
        if (order.getStatus() == AWAITING_APPROVAL) {
            OrderStatus to = decision == ApprovalDecision.APPROVED ? IN_REPAIR : READY_FOR_PICKUP;
            OrderOutcome outcome = decision == ApprovalDecision.APPROVED ? null : OrderOutcome.NOT_REPAIRED;
            return Optional.of(transition(order, to, outcome, actor, note, now));
        }
        if (order.getApprovalDecision() == decision) {
            return Optional.empty();
        }
        if (order.getApprovalDecision() == ApprovalDecision.APPROVED || order.getApprovalDecision() == ApprovalDecision.DECLINED) {
            throw new InvalidOrderTransitionException("O orçamento já foi " + decisionLabel(order.getApprovalDecision()));
        }
        throw new InvalidOrderTransitionException("O pedido não está aguardando aprovação");
    }

    private static OrderOutcome resolveOutcome(OrderStatus from, OrderStatus to, OrderOutcome requested) {
        if (to != READY_FOR_PICKUP) {
            if (requested != null) {
                throw new InvalidOrderTransitionException("Resultado só pode ser informado ao marcar como pronto para retirada");
            }
            return null;
        }
        if (from == IN_REPAIR) {
            return requested == null ? OrderOutcome.REPAIRED : requested;
        }
        if (requested == OrderOutcome.REPAIRED) {
            throw new InvalidOrderTransitionException("Pedido sem conserto aprovado só pode sair como NOT_REPAIRED");
        }
        return OrderOutcome.NOT_REPAIRED;
    }

    private static void sendBudget(Order order, Instant now) {
        double total = order.calculateTotal();
        if (total <= 0) {
            throw new OrderBudgetEmptyException();
        }
        order.setTotal(total);
        order.setBudgetLaborTotal(order.calculateLaborTotal());
        order.setBudgetPartsTotal(order.calculatePartsTotal());
        order.setBudgetDiscount(order.getDiscount() == null ? 0.0 : order.getDiscount());
        order.setBudgetTotal(total);
        order.setBudgetSentAt(now);
        order.setApprovalDecision(ApprovalDecision.PENDING);
        order.setDecidedAt(null);
        order.setDecidedVia(null);
    }

    private static void decide(Order order, ApprovalDecision decision, OrderActor actor, Instant now) {
        order.setApprovalDecision(decision);
        order.setDecidedAt(now);
        order.setDecidedVia(actor);
    }

    private static String clean(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String trimmed = note.strip();
        return trimmed.length() <= NOTE_MAX ? trimmed : trimmed.substring(0, NOTE_MAX);
    }

    public static String label(OrderStatus status) {
        if (status == null) {
            return "sem situação";
        }
        return switch (status) {
            case RECEIVED -> "Em avaliação";
            case AWAITING_APPROVAL -> "Aguardando aprovação";
            case IN_REPAIR -> "Em conserto";
            case READY_FOR_PICKUP -> "Pronto para retirada";
            case DELIVERED -> "Entregue";
        };
    }

    private static String decisionLabel(ApprovalDecision decision) {
        return decision == ApprovalDecision.APPROVED ? "aprovado" : "recusado";
    }
}
