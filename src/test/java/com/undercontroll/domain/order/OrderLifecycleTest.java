package com.undercontroll.domain.order;

import com.undercontroll.domain.enums.ApprovalDecision;
import com.undercontroll.domain.enums.OrderActor;
import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.exception.InvalidOrderTransitionException;
import com.undercontroll.domain.exception.OrderBudgetEmptyException;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.model.OrderStatusEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.undercontroll.domain.enums.OrderStatus.AWAITING_APPROVAL;
import static com.undercontroll.domain.enums.OrderStatus.DELIVERED;
import static com.undercontroll.domain.enums.OrderStatus.IN_REPAIR;
import static com.undercontroll.domain.enums.OrderStatus.READY_FOR_PICKUP;
import static com.undercontroll.domain.enums.OrderStatus.RECEIVED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderLifecycleTest {

    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");
    private static final Set<String> VALID = Set.of(
            "RECEIVED>AWAITING_APPROVAL", "RECEIVED>READY_FOR_PICKUP",
            "AWAITING_APPROVAL>IN_REPAIR", "AWAITING_APPROVAL>READY_FOR_PICKUP", "AWAITING_APPROVAL>RECEIVED",
            "IN_REPAIR>READY_FOR_PICKUP", "READY_FOR_PICKUP>DELIVERED");

    static Stream<Arguments> allPairs() {
        return Arrays.stream(OrderStatus.values())
                .flatMap(from -> Arrays.stream(OrderStatus.values()).map(to -> Arguments.of(from, to)));
    }

    static Order order(OrderStatus status) {
        return Order.builder()
                .id(1)
                .status(status)
                .discount(10.0)
                .orderItems(new ArrayList<>(List.of(OrderItem.builder().laborValue(150.0).build())))
                .demands(new ArrayList<>(List.of(Demand.builder().quantity(2L)
                        .component(ComponentPart.builder().price(20.0).build()).build())))
                .build();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("allPairs")
    @DisplayName("only the documented transitions are allowed")
    void everyPair(OrderStatus from, OrderStatus to) {
        Order order = order(from);
        if (VALID.contains(from + ">" + to)) {
            OrderStatusEvent event = OrderLifecycle.transition(order, to, null, OrderActor.SHOP, " nota ", NOW);
            assertEquals(to, order.getStatus());
            assertEquals(from, event.from());
            assertEquals(to, event.to());
            assertEquals("nota", event.note());
        } else {
            assertThrows(InvalidOrderTransitionException.class,
                    () -> OrderLifecycle.transition(order, to, null, OrderActor.SHOP, null, NOW));
            assertEquals(from, order.getStatus());
        }
    }

    @Test
    @DisplayName("sending the budget snapshots values and leaves the decision pending")
    void sendBudget() {
        Order order = order(RECEIVED);

        OrderLifecycle.transition(order, AWAITING_APPROVAL, null, OrderActor.SHOP, null, NOW);

        assertEquals(150.0, order.getBudgetLaborTotal());
        assertEquals(40.0, order.getBudgetPartsTotal());
        assertEquals(10.0, order.getBudgetDiscount());
        assertEquals(180.0, order.getBudgetTotal());
        assertEquals(180.0, order.getTotal());
        assertEquals(NOW, order.getBudgetSentAt());
        assertEquals(ApprovalDecision.PENDING, order.getApprovalDecision());
    }

    @Test
    @DisplayName("an empty budget cannot be sent")
    void emptyBudget() {
        Order order = Order.builder().id(1).status(RECEIVED).discount(0.0).build();

        assertThrows(OrderBudgetEmptyException.class,
                () -> OrderLifecycle.transition(order, AWAITING_APPROVAL, null, OrderActor.SHOP, null, NOW));
        assertEquals(RECEIVED, order.getStatus());
    }

    @Test
    @DisplayName("outcome rules for READY_FOR_PICKUP")
    void outcomes() {
        Order repaired = order(IN_REPAIR);
        OrderLifecycle.transition(repaired, READY_FOR_PICKUP, null, OrderActor.SHOP, null, NOW);
        assertEquals(OrderOutcome.REPAIRED, repaired.getOutcome());
        assertEquals(NOW, repaired.getCompletedAt());

        Order failed = order(IN_REPAIR);
        OrderLifecycle.transition(failed, READY_FOR_PICKUP, OrderOutcome.NOT_REPAIRED, OrderActor.SHOP, null, NOW);
        assertEquals(OrderOutcome.NOT_REPAIRED, failed.getOutcome());

        Order notEvaluated = order(RECEIVED);
        OrderLifecycle.transition(notEvaluated, READY_FOR_PICKUP, null, OrderActor.SHOP, null, NOW);
        assertEquals(OrderOutcome.NOT_REPAIRED, notEvaluated.getOutcome());

        assertThrows(InvalidOrderTransitionException.class, () -> OrderLifecycle.transition(
                order(RECEIVED), READY_FOR_PICKUP, OrderOutcome.REPAIRED, OrderActor.SHOP, null, NOW));
        assertThrows(InvalidOrderTransitionException.class, () -> OrderLifecycle.transition(
                order(RECEIVED), AWAITING_APPROVAL, OrderOutcome.REPAIRED, OrderActor.SHOP, null, NOW));
    }

    @Test
    @DisplayName("budget decisions move the order, record who decided and are idempotent")
    void decisions() {
        Order approved = order(AWAITING_APPROVAL);
        OrderStatusEvent event = OrderLifecycle.decideBudget(approved, ApprovalDecision.APPROVED, OrderActor.CUSTOMER_LINK, null, NOW).orElseThrow();
        assertEquals(IN_REPAIR, approved.getStatus());
        assertEquals(ApprovalDecision.APPROVED, approved.getApprovalDecision());
        assertEquals(OrderActor.CUSTOMER_LINK, approved.getDecidedVia());
        assertEquals(OrderActor.CUSTOMER_LINK, event.actor());
        assertTrue(OrderLifecycle.decideBudget(approved, ApprovalDecision.APPROVED, OrderActor.SHOP, null, NOW).isEmpty());
        assertThrows(InvalidOrderTransitionException.class,
                () -> OrderLifecycle.decideBudget(approved, ApprovalDecision.DECLINED, OrderActor.SHOP, null, NOW));

        Order declined = order(AWAITING_APPROVAL);
        OrderLifecycle.decideBudget(declined, ApprovalDecision.DECLINED, OrderActor.SHOP, null, NOW);
        assertEquals(READY_FOR_PICKUP, declined.getStatus());
        assertEquals(OrderOutcome.NOT_REPAIRED, declined.getOutcome());
        assertEquals(ApprovalDecision.DECLINED, declined.getApprovalDecision());

        assertThrows(InvalidOrderTransitionException.class,
                () -> OrderLifecycle.decideBudget(order(RECEIVED), ApprovalDecision.APPROVED, OrderActor.SHOP, null, NOW));
        assertThrows(InvalidOrderTransitionException.class,
                () -> OrderLifecycle.decideBudget(order(AWAITING_APPROVAL), ApprovalDecision.PENDING, OrderActor.SHOP, null, NOW));
    }

    @Test
    @DisplayName("redoing the budget clears the decision; delivering stamps deliveredAt")
    void redoAndDeliver() {
        Order order = order(AWAITING_APPROVAL);
        order.setApprovalDecision(ApprovalDecision.PENDING);
        OrderLifecycle.transition(order, RECEIVED, null, OrderActor.SHOP, null, NOW);
        assertNull(order.getApprovalDecision());

        Order ready = order(READY_FOR_PICKUP);
        ready.setOutcome(OrderOutcome.REPAIRED);
        OrderLifecycle.transition(ready, DELIVERED, null, OrderActor.SHOP, null, NOW);
        assertEquals(NOW, ready.getDeliveredAt());
        assertEquals(OrderOutcome.REPAIRED, ready.getOutcome());
    }
}
