package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.gateway.AnnouncementGateway;
import com.undercontroll.domain.gateway.ComponentGateway;
import com.undercontroll.domain.gateway.DemandGateway;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.gateway.ShopSnapshotLoader;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.chat.ShopSnapshot;
import com.undercontroll.domain.model.chat.ShopSnapshotFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ShopSnapshotLoaderImpl implements ShopSnapshotLoader {

    private final OrderGateway orderGateway;
    private final ComponentGateway componentGateway;
    private final DemandGateway demandGateway;
    private final AnnouncementGateway announcementGateway;

    @Override
    @Cacheable(value = "anaShopSnapshot", key = "'current'")
    public ShopSnapshot load() {
        List<Order> orders = new ArrayList<>();
        orders.addAll(orderGateway.findOpenRepairs(ShopSnapshotFactory.OPEN_LIMIT));
        orders.addAll(orderGateway.findReadyForPickup(ShopSnapshotFactory.PICKUP_LIMIT));
        return ShopSnapshotFactory.from(
                orders,
                componentGateway.findLowStock(
                        ShopSnapshotFactory.LOW_STOCK_MAX,
                        ShopSnapshotFactory.STOCK_LIMIT
                ),
                demandGateway.findRecent(ShopSnapshotFactory.DEMAND_LIMIT),
                announcementGateway.findLastAnnouncement().orElse(null),
                orderGateway.countByStatusIn(List.of(OrderStatus.PENDING, OrderStatus.IN_ANALYSIS)),
                orderGateway.countByStatusIn(List.of(OrderStatus.COMPLETED)),
                LocalDateTime.now(ZoneOffset.UTC)
        );
    }
}
