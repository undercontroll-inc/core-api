package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.gateway.OrderPhotoGateway;
import com.undercontroll.domain.gateway.OrderStatusEventGateway;
import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderPhoto;
import com.undercontroll.domain.model.ShopProfile;
import com.undercontroll.domain.usecase.order.OrderOutput;
import com.undercontroll.domain.usecase.order.OrderPhotoOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderDetailsAssembler {

    private final OrderStatusEventGateway statusEvents;
    private final OrderPhotoGateway photos;
    private final ObjectStorageGateway storage;
    private final ShopProfile shopProfile;

    public OrderOutput summary(Order order, boolean includeTechnical) {
        return OrderOutput.from(order, includeTechnical, shopProfile);
    }

    public OrderOutput detail(Order order, boolean includeTechnical) {
        return summary(order, includeTechnical).withDetails(
                statusEvents.findByOrderId(order.getId()),
                photos(order.getId())
        );
    }

    public List<OrderPhotoOutput> photos(Integer orderId) {
        return photos.findByOrderId(orderId).stream().map(this::photo).toList();
    }

    public OrderPhotoOutput photo(OrderPhoto photo) {
        return new OrderPhotoOutput(photo.id(), photo.kind(), storage.presignRead(StorageArea.ORDER_PHOTOS, photo.storageKey()),
                photo.createdAt());
    }
}
