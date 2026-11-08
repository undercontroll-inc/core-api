package com.undercontroll.domain.gateway;

import com.undercontroll.domain.enums.OrderPhotoKind;
import com.undercontroll.domain.model.OrderPhoto;

import java.util.List;
import java.util.Optional;

public interface OrderPhotoGateway {

    OrderPhoto save(OrderPhoto photo);

    List<OrderPhoto> findByOrderId(Integer orderId);

    Optional<OrderPhoto> findFirstByOrderIdAndKind(Integer orderId, OrderPhotoKind kind);

    Optional<OrderPhoto> findById(Integer orderId, Long photoId);

    long countByOrderId(Integer orderId);

    Optional<OrderPhoto> findByStorageKey(String storageKey);

    void delete(Long photoId);
}
