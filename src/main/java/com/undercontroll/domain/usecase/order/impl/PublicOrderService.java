package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.enums.ApprovalDecision;
import com.undercontroll.domain.enums.OrderActor;
import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderPhotoKind;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.exception.InvalidOrderTransitionException;
import com.undercontroll.domain.exception.OrderConcurrentUpdateException;
import com.undercontroll.domain.exception.TrackingNotFoundException;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.gateway.OrderPhotoGateway;
import com.undercontroll.domain.gateway.OrderStatusEventGateway;
import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.model.OrderStatusEvent;
import com.undercontroll.domain.model.ShopProfile;
import com.undercontroll.domain.order.OrderMessages;
import com.undercontroll.domain.order.TrackingTokens;
import com.undercontroll.domain.usecase.order.DecidePublicOrderPort;
import com.undercontroll.domain.usecase.order.GetPublicOrderPort;
import com.undercontroll.domain.usecase.order.PublicOrderView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PublicOrderService implements GetPublicOrderPort, DecidePublicOrderPort {

    private final OrderGateway orderGateway;
    private final OrderStatusEventGateway statusEvents;
    private final OrderPhotoGateway photos;
    private final ObjectStorageGateway storage;
    private final OrderLifecycleService lifecycle;
    private final ShopProfile shop;
    private final Clock clock;
    private final Duration linkTtlAfterDelivery;

    public PublicOrderService(
            OrderGateway orderGateway,
            OrderStatusEventGateway statusEvents,
            OrderPhotoGateway photos,
            ObjectStorageGateway storage,
            OrderLifecycleService lifecycle,
            ShopProfile shop,
            Clock clock,
            @Value("${undercontroll.public.link-ttl-after-delivery:60d}") Duration linkTtlAfterDelivery
    ) {
        this.orderGateway = orderGateway;
        this.statusEvents = statusEvents;
        this.photos = photos;
        this.storage = storage;
        this.lifecycle = lifecycle;
        this.shop = shop;
        this.clock = clock;
        this.linkTtlAfterDelivery = linkTtlAfterDelivery;
    }

    @Override
    public PublicOrderView execute(String token) {
        return view(find(token));
    }

    @Override
    public PublicOrderView execute(String token, ApprovalDecision decision) {
        Order order = find(token);
        if (decision == ApprovalDecision.APPROVED || decision == ApprovalDecision.DECLINED) {
            try {
                lifecycle.decide(order.getId(), decision, OrderActor.CUSTOMER_LINK, null);
            } catch (InvalidOrderTransitionException | OrderConcurrentUpdateException ex) {
                log.info("Public decision not applied orderId={} decision={} cause={}", order.getId(), decision, ex.getMessage());
            }
        }
        return view(find(token));
    }

    Order find(String token) {
        if (!TrackingTokens.wellFormed(token)) {
            throw new TrackingNotFoundException();
        }
        Order order = orderGateway.findByTrackingToken(token).orElseThrow(TrackingNotFoundException::new);
        if (order.getStatus() == OrderStatus.DELIVERED && order.getDeliveredAt() != null
                && order.getDeliveredAt().plus(linkTtlAfterDelivery).isBefore(clock.instant())) {
            throw new TrackingNotFoundException();
        }
        return order;
    }

    PublicOrderView view(Order order) {
        OrderItem item = order.getOrderItems().isEmpty() ? null : order.getOrderItems().getFirst();
        String devicePhotoUrl = photos.findFirstByOrderIdAndKind(order.getId(), OrderPhotoKind.DEVICE)
                .map(photo -> storage.presignRead(StorageArea.ORDER_PHOTOS, photo.storageKey()))
                .orElse(null);
        String whatsappDigits = shop.whatsappDigits();
        return new PublicOrderView(
                order.getTrackingToken(),
                order.getId(),
                OrderMessages.firstName(order.getUser()),
                item == null ? null : new PublicOrderView.Device(item.getType(), item.getBrand(), item.getModel()),
                order.getStatus(),
                statusLabel(order),
                order.getOutcome(),
                timeline(order),
                devicePhotoUrl,
                order.getBudgetSentAt() == null ? null : new PublicOrderView.Budget(
                        order.getBudgetLaborTotal(),
                        order.getBudgetPartsTotal(),
                        order.getBudgetDiscount(),
                        order.getBudgetTotal(),
                        order.isReturnGuarantee() && shop.warrantyDays() > 0
                                ? "Garantia de " + shop.warrantyDays() + " dias no conserto" : null),
                order.getApprovalDecision() == ApprovalDecision.PENDING ? null : order.getApprovalDecision(),
                order.getDecidedAt(),
                order.getStatus() == OrderStatus.AWAITING_APPROVAL,
                new PublicOrderView.Shop(
                        shop.name(),
                        blankToNull(shop.phone()),
                        blankToNull(shop.phoneDigits()),
                        whatsappDigits.isBlank() ? null : "https://wa.me/" + whatsappDigits,
                        blankToNull(shop.address()),
                        blankToNull(shop.hours()))
        );
    }

    static String statusLabel(Order order) {
        return switch (order.getStatus()) {
            case RECEIVED -> "Em avaliação";
            case AWAITING_APPROVAL -> "Aguardando sua aprovação";
            case IN_REPAIR -> "Em conserto";
            case READY_FOR_PICKUP -> order.getOutcome() == OrderOutcome.NOT_REPAIRED
                    ? "Pode buscar seu aparelho" : "Pronto! Pode buscar seu aparelho";
            case DELIVERED -> "Entregue";
        };
    }

    private List<PublicOrderView.TimelineEntry> timeline(Order order) {
        List<PublicOrderView.TimelineEntry> entries = new ArrayList<>();
        LocalDate received = order.getReceived_at() != null ? order.getReceived_at()
                : order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate() : null;
        entries.add(new PublicOrderView.TimelineEntry("Aparelho recebido", received));
        for (OrderStatusEvent event : statusEvents.findByOrderId(order.getId())) {
            String label = timelineLabel(event);
            if (label != null) {
                entries.add(new PublicOrderView.TimelineEntry(label, date(event.createdAt())));
            }
        }
        return entries;
    }

    private static String timelineLabel(OrderStatusEvent event) {
        return switch (event.to()) {
            case RECEIVED -> "Orçamento em revisão";
            case AWAITING_APPROVAL -> "Orçamento enviado";
            case IN_REPAIR -> "Orçamento aprovado. Conserto iniciado";
            case READY_FOR_PICKUP -> event.from() == OrderStatus.AWAITING_APPROVAL
                    ? "Orçamento recusado. Aparelho disponível para retirada"
                    : event.outcome() == OrderOutcome.NOT_REPAIRED
                    ? "Não foi possível consertar. Aparelho disponível para retirada"
                    : "Conserto concluído";
            case DELIVERED -> "Aparelho entregue";
        };
    }

    private LocalDate date(Instant instant) {
        return instant == null ? null : instant.atZone(shop.zone()).toLocalDate();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
