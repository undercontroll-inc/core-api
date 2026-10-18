package com.undercontroll.domain.model.chat;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.model.chat.ShopSnapshot.DemandFact;
import com.undercontroll.domain.model.chat.ShopSnapshot.RepairFact;
import com.undercontroll.domain.model.chat.ShopSnapshot.StockFact;
import com.undercontroll.domain.support.Texts;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class ShopSnapshotFactory {

    public static final int OPEN_LIMIT = 5;
    public static final int PICKUP_LIMIT = 3;
    public static final int STOCK_LIMIT = 5;
    public static final int DEMAND_LIMIT = 5;
    public static final long LOW_STOCK_MAX = 5L;

    private static final int APPLIANCE_MAX_LENGTH = 28;

    private ShopSnapshotFactory() {
    }

    public static ShopSnapshot from(
            List<Order> orders,
            List<ComponentPart> parts,
            List<Demand> demands,
            Announcement lastAnnouncement
    ) {
        ShopSnapshot base = build(orders, parts, demands, lastAnnouncement);
        long openTotal = count(orders, OrderStatus.PENDING, OrderStatus.IN_ANALYSIS);
        long pickupTotal = count(orders, OrderStatus.COMPLETED);
        return withTotals(base, openTotal, pickupTotal, null);
    }

    public static ShopSnapshot from(
            List<Order> orders,
            List<ComponentPart> parts,
            List<Demand> demands,
            Announcement lastAnnouncement,
            long openTotal,
            long pickupTotal,
            LocalDateTime takenAt
    ) {
        return withTotals(build(orders, parts, demands, lastAnnouncement), openTotal, pickupTotal, takenAt);
    }

    private static ShopSnapshot withTotals(ShopSnapshot base, long openTotal, long pickupTotal, LocalDateTime takenAt) {
        return new ShopSnapshot(
                base.openRepairs(),
                base.readyForPickup(),
                base.lowStockParts(),
                base.pendingParts(),
                base.lastAnnouncementTitle(),
                Math.max(openTotal, base.openRepairs().size()),
                Math.max(pickupTotal, base.readyForPickup().size()),
                takenAt
        );
    }

    private static long count(List<Order> orders, OrderStatus... statuses) {
        if (orders == null) {
            return 0;
        }
        List<OrderStatus> wanted = Arrays.asList(statuses);
        return orders.stream().filter(order -> wanted.contains(order.getStatus())).count();
    }

    private static ShopSnapshot build(
            List<Order> orders,
            List<ComponentPart> parts,
            List<Demand> demands,
            Announcement lastAnnouncement
    ) {
        List<Order> safeOrders = orders == null ? List.of() : orders;
        List<RepairFact> open = safeOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PENDING
                        || order.getStatus() == OrderStatus.IN_ANALYSIS)
                .sorted(openRepairOrder())
                .limit(OPEN_LIMIT)
                .map(ShopSnapshotFactory::toRepairFact)
                .toList();
        List<RepairFact> pickup = safeOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.COMPLETED)
                .sorted(pickupOrder())
                .limit(PICKUP_LIMIT)
                .map(ShopSnapshotFactory::toRepairFact)
                .toList();
        List<StockFact> stock = (parts == null ? List.<ComponentPart>of() : parts).stream()
                .filter(part -> part.getQuantity() != null && part.getQuantity() <= LOW_STOCK_MAX)
                .sorted(Comparator.comparing(ComponentPart::getQuantity))
                .limit(STOCK_LIMIT)
                .map(part -> new StockFact(part.getName(), part.getQuantity()))
                .filter(fact -> fact.name() != null && !fact.name().isBlank())
                .toList();
        List<DemandFact> pending = (demands == null ? List.<Demand>of() : demands).stream()
                .map(ShopSnapshotFactory::toDemandFact)
                .filter(Objects::nonNull)
                .limit(DEMAND_LIMIT)
                .toList();
        String announcement = lastAnnouncement == null ? null : Texts.blankToNull(lastAnnouncement.getTitle());
        return new ShopSnapshot(open, pickup, stock, pending, announcement);
    }

    static RepairFact toRepairFact(Order order) {
        return new RepairFact(
                order.getId(),
                order.getUser() == null ? null : order.getUser().fullName(),
                applianceOf(order),
                statusLabel(order.getStatus())
        );
    }

    static DemandFact toDemandFact(Demand demand) {
        if (demand == null || demand.getComponent() == null) {
            return null;
        }
        String name = Texts.blankToNull(demand.getComponent().getName());
        if (name == null) {
            return null;
        }
        Integer orderId = demand.getOrder() == null ? null : demand.getOrder().getId();
        return new DemandFact(name, orderId, demand.getQuantity());
    }

    private static String applianceOf(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return Texts.clip(Texts.blankToNull(order.getCustomerDescription()), APPLIANCE_MAX_LENGTH);
        }
        OrderItem item = order.getOrderItems().getFirst();
        String type = Texts.blankToNull(item.getType());
        String brand = Texts.blankToNull(item.getBrand());
        if (type != null) {
            type = type.toLowerCase(Locale.ROOT);
        }
        if (type != null && brand != null) {
            return Texts.clip(type + " " + brand, APPLIANCE_MAX_LENGTH);
        }
        if (type != null) {
            return Texts.clip(type, APPLIANCE_MAX_LENGTH);
        }
        if (brand != null) {
            return Texts.clip(brand, APPLIANCE_MAX_LENGTH);
        }
        return Texts.clip(Texts.blankToNull(item.getModel()), APPLIANCE_MAX_LENGTH);
    }

    private static String statusLabel(OrderStatus status) {
        if (status == null) {
            return "em andamento";
        }
        return switch (status) {
            case PENDING -> RepairFact.NOT_LOOKED_AT;
            case IN_ANALYSIS -> "em análise";
            case COMPLETED -> "pronto para buscar";
            case DELIVERED -> "entregue";
        };
    }

    private static Comparator<Order> openRepairOrder() {
        return Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(Order::getId, Comparator.nullsLast(Integer::compareTo));
    }

    private static Comparator<Order> pickupOrder() {
        return Comparator.comparing(Order::getCompletedTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Order::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
