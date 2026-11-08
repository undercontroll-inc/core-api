package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.enums.ApprovalDecision;
import com.undercontroll.domain.enums.OrderActor;
import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderPhotoKind;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.event.OrderBudgetDecidedEvent;
import com.undercontroll.domain.event.OrderStatusChangedEvent;
import com.undercontroll.domain.exception.InvalidOrderTransitionException;
import com.undercontroll.domain.exception.OrderConcurrentUpdateException;
import com.undercontroll.domain.exception.TrackingNotFoundException;
import com.undercontroll.domain.gateway.MetricName;
import com.undercontroll.domain.gateway.MetricsGateway;
import com.undercontroll.domain.gateway.OrderEventPublisher;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.gateway.OrderPhotoGateway;
import com.undercontroll.domain.gateway.OrderStatusEventGateway;
import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.model.OrderPhoto;
import com.undercontroll.domain.model.OrderStatusEvent;
import com.undercontroll.domain.model.ShopProfile;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.order.TrackingTokens;
import com.undercontroll.domain.usecase.order.PublicOrderView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderLifecycleServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");
    private static final ShopProfile SHOP = new ShopProfile("Pelluci", "(11) 4000-1234", "5511999990000", "Rua A, 10",
            "Seg a sex, 9h às 18h", 90, ZoneId.of("America/Sao_Paulo"), "https://loja.example");

    private OrderGateway orders;
    private OrderStatusEventGateway events;
    private OrderEventPublisher publisher;
    private MetricsGateway metrics;
    private OrderPhotoGateway photos;
    private ObjectStorageGateway storage;
    private OrderLifecycleService service;
    private PublicOrderService publicService;

    @BeforeEach
    void setUp() {
        orders = mock(OrderGateway.class);
        events = mock(OrderStatusEventGateway.class);
        publisher = mock(OrderEventPublisher.class);
        metrics = mock(MetricsGateway.class);
        photos = mock(OrderPhotoGateway.class);
        storage = mock(ObjectStorageGateway.class);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new OrderLifecycleService(orders, events, publisher, metrics, clock);
        publicService = new PublicOrderService(orders, events, photos, storage, service, SHOP, clock, Duration.ofDays(60));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(events.findByOrderId(any())).thenReturn(List.of());
        when(photos.findFirstByOrderIdAndKind(any(), any())).thenReturn(Optional.empty());
    }

    private static Order order(OrderStatus status) {
        return Order.builder()
                .id(123)
                .status(status)
                .discount(0.0)
                .returnGuarantee(true)
                .user(User.builder().id(7).name("Maria").lastName("Souza").cpf("12345678900").phone("11988887777").address("Rua X").build())
                .orderItems(new ArrayList<>(List.of(OrderItem.builder().type("Liquidificador").brand("Arno").model("LN50").laborValue(180.0).build())))
                .build();
    }

    @Test
    @DisplayName("sending the budget creates a token, records history and publishes the status change")
    void sendBudget() {
        Order order = order(OrderStatus.RECEIVED);
        when(orders.findById(123)).thenReturn(Optional.of(order));

        Order sent = service.transition(123, OrderStatus.AWAITING_APPROVAL, null, OrderActor.SHOP, null);

        assertTrue(TrackingTokens.wellFormed(sent.getTrackingToken()));
        assertEquals(180.0, sent.getBudgetTotal());
        ArgumentCaptor<OrderStatusEvent> event = ArgumentCaptor.forClass(OrderStatusEvent.class);
        verify(events).save(event.capture());
        assertEquals(OrderStatus.RECEIVED, event.getValue().from());
        verify(publisher).statusChanged(new OrderStatusChangedEvent(123, OrderStatus.RECEIVED, OrderStatus.AWAITING_APPROVAL, null, OrderActor.SHOP, NOW));
        verify(publisher, never()).budgetDecided(any());
    }

    @Test
    @DisplayName("a decision publishes budget.decided and counts metrics; repeating it changes nothing")
    void decide() {
        Order order = order(OrderStatus.AWAITING_APPROVAL);
        order.setBudgetTotal(180.0);
        when(orders.findById(123)).thenReturn(Optional.of(order));

        service.decide(123, ApprovalDecision.DECLINED, OrderActor.CUSTOMER_LINK, null);
        service.decide(123, ApprovalDecision.DECLINED, OrderActor.CUSTOMER_LINK, null);

        assertEquals(OrderStatus.READY_FOR_PICKUP, order.getStatus());
        verify(publisher).budgetDecided(new OrderBudgetDecidedEvent(123, ApprovalDecision.DECLINED, OrderActor.CUSTOMER_LINK, 180.0, NOW));
        verify(metrics).increment(MetricName.ORDER_BUDGET_DECLINED);
        verify(events).save(any());
    }

    @Test
    @DisplayName("finishing a repair counts ORDER_COMPLETED")
    void completed() {
        when(orders.findById(123)).thenReturn(Optional.of(order(OrderStatus.IN_REPAIR)));

        service.transition(123, OrderStatus.READY_FOR_PICKUP, null, OrderActor.SHOP, "Trocado o motor");

        verify(metrics).increment(MetricName.ORDER_COMPLETED);
    }

    @Test
    @DisplayName("the public view shows only first name, device, values and status — never CPF, phone or address")
    void publicView() {
        Order order = order(OrderStatus.AWAITING_APPROVAL);
        order.setTrackingToken("7Hq2xLmP9vKa3TzR8dNw4c");
        order.setBudgetSentAt(NOW);
        order.setBudgetLaborTotal(180.0);
        order.setBudgetPartsTotal(0.0);
        order.setBudgetDiscount(0.0);
        order.setBudgetTotal(180.0);
        order.setTechnicalDescription("Motor queimado, cliente difícil");
        when(orders.findByTrackingToken("7Hq2xLmP9vKa3TzR8dNw4c")).thenReturn(Optional.of(order));
        when(photos.findFirstByOrderIdAndKind(123, OrderPhotoKind.DEVICE)).thenReturn(Optional.of(
                new OrderPhoto(1L, 123, OrderPhotoKind.DEVICE, "order-photos/123/a.jpg", "image/jpeg", 10, NOW)));
        when(storage.presignRead(StorageArea.ORDER_PHOTOS, "order-photos/123/a.jpg")).thenReturn("https://s3/photo");

        PublicOrderView view = publicService.execute("7Hq2xLmP9vKa3TzR8dNw4c");

        assertEquals("Maria", view.customerFirstName());
        assertEquals("Aguardando sua aprovação", view.statusLabel());
        assertTrue(view.canDecide());
        assertEquals(180.0, view.budget().total());
        assertEquals("Garantia de 90 dias no conserto", view.budget().warranty());
        assertEquals("https://s3/photo", view.devicePhotoUrl());
        assertEquals("https://wa.me/5511999990000", view.shop().whatsappUrl());
        String all = view.toString();
        assertFalse(all.contains("Souza"));
        assertFalse(all.contains("12345678900"));
        assertFalse(all.contains("11988887777"));
        assertFalse(all.contains("Rua X"));
        assertFalse(all.contains("Motor queimado"));
    }

    @Test
    @DisplayName("customer decision through the link is applied once; later attempts show the recorded decision")
    void publicDecision() {
        Order order = order(OrderStatus.AWAITING_APPROVAL);
        order.setTrackingToken("7Hq2xLmP9vKa3TzR8dNw4c");
        when(orders.findByTrackingToken("7Hq2xLmP9vKa3TzR8dNw4c")).thenReturn(Optional.of(order));
        when(orders.findById(123)).thenReturn(Optional.of(order));

        PublicOrderView first = publicService.execute("7Hq2xLmP9vKa3TzR8dNw4c", ApprovalDecision.APPROVED);
        PublicOrderView second = publicService.execute("7Hq2xLmP9vKa3TzR8dNw4c", ApprovalDecision.DECLINED);

        assertEquals(ApprovalDecision.APPROVED, first.approvalDecision());
        assertEquals(OrderActor.CUSTOMER_LINK, order.getDecidedVia());
        assertFalse(first.canDecide());
        assertEquals(ApprovalDecision.APPROVED, second.approvalDecision());
        assertEquals(OrderStatus.IN_REPAIR, second.status());
    }

    @Test
    @DisplayName("losing the race to the shop still shows the page")
    void publicDecisionConflict() {
        Order order = order(OrderStatus.AWAITING_APPROVAL);
        order.setTrackingToken("7Hq2xLmP9vKa3TzR8dNw4c");
        when(orders.findByTrackingToken("7Hq2xLmP9vKa3TzR8dNw4c")).thenReturn(Optional.of(order));
        when(orders.findById(123)).thenReturn(Optional.of(order));
        when(orders.save(any(Order.class))).thenThrow(new OrderConcurrentUpdateException(123, null));

        assertNotNull(publicService.execute("7Hq2xLmP9vKa3TzR8dNw4c", ApprovalDecision.APPROVED));
    }

    @Test
    @DisplayName("unknown, malformed and expired links are not found")
    void notFound() {
        Order delivered = order(OrderStatus.DELIVERED);
        delivered.setTrackingToken("7Hq2xLmP9vKa3TzR8dNw4c");
        delivered.setDeliveredAt(NOW.minus(Duration.ofDays(61)));
        when(orders.findByTrackingToken("7Hq2xLmP9vKa3TzR8dNw4c")).thenReturn(Optional.of(delivered));
        when(orders.findByTrackingToken("AAAAAAAAAAAAAAAAAAAAAA")).thenReturn(Optional.empty());

        assertThrows(TrackingNotFoundException.class, () -> publicService.execute("7Hq2xLmP9vKa3TzR8dNw4c"));
        assertThrows(TrackingNotFoundException.class, () -> publicService.execute("AAAAAAAAAAAAAAAAAAAAAA"));
        assertThrows(TrackingNotFoundException.class, () -> publicService.execute("../../etc"));

        delivered.setDeliveredAt(NOW.minus(Duration.ofDays(10)));
        assertEquals("Entregue", publicService.execute("7Hq2xLmP9vKa3TzR8dNw4c").statusLabel());
    }

    @Test
    @DisplayName("an invalid transition does not save or publish")
    void invalid() {
        when(orders.findById(123)).thenReturn(Optional.of(order(OrderStatus.RECEIVED)));

        assertThrows(InvalidOrderTransitionException.class,
                () -> service.transition(123, OrderStatus.DELIVERED, null, OrderActor.SHOP, null));
        verify(orders, never()).save(any());
        verify(publisher, never()).statusChanged(any());
        assertNull(order(OrderStatus.RECEIVED).getOutcome());
        assertEquals(OrderOutcome.REPAIRED, OrderOutcome.valueOf("REPAIRED"));
    }
}
