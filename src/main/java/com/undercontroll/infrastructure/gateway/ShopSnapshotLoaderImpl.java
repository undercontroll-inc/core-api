package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.gateway.AnnouncementGateway;
import com.undercontroll.domain.gateway.ComponentGateway;
import com.undercontroll.domain.gateway.DemandGateway;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.gateway.ShopSnapshotLoader;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.chat.ShopSnapshot;
import com.undercontroll.domain.model.chat.ShopSuggestionComposer;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

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
        orders.addAll(orderGateway.findOpenRepairs(ShopSuggestionComposer.OPEN_LIMIT));
        orders.addAll(orderGateway.findReadyForPickup(ShopSuggestionComposer.PICKUP_LIMIT));
        return ShopSuggestionComposer.from(
                orders,
                componentGateway.findLowStock(
                        ShopSuggestionComposer.LOW_STOCK_MAX,
                        ShopSuggestionComposer.STOCK_LIMIT
                ),
                demandGateway.findRecent(ShopSuggestionComposer.DEMAND_LIMIT),
                announcementGateway.findLastAnnouncement().orElse(null)
        );
    }
}
