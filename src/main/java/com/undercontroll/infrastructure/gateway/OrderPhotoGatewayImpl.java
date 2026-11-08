package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.enums.OrderPhotoKind;
import com.undercontroll.domain.gateway.OrderPhotoGateway;
import com.undercontroll.domain.model.OrderPhoto;
import com.undercontroll.infrastructure.persistence.entity.OrderPhotoJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.OrderPhotoJpaRepository;
import com.undercontroll.infrastructure.persistence.repository.OrderStatusEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderPhotoGatewayImpl implements OrderPhotoGateway {

    private final OrderPhotoJpaRepository photos;

    @Override
    @Transactional
    public OrderPhoto save(OrderPhoto photo) {
        return toDomain(photos.save(OrderPhotoJpaEntity.builder()
                .orderId(photo.orderId())
                .kind(photo.kind())
                .storageKey(photo.storageKey())
                .contentType(photo.contentType())
                .size(photo.size())
                .createdAt(photo.createdAt())
                .build()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderPhoto> findByOrderId(Integer orderId) {
        return photos.findByOrderIdOrderByCreatedAtAscIdAsc(orderId).stream().map(OrderPhotoGatewayImpl::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderPhoto> findFirstByOrderIdAndKind(Integer orderId, OrderPhotoKind kind) {
        return photos.findFirstByOrderIdAndKindOrderByCreatedAtAscIdAsc(orderId, kind).map(OrderPhotoGatewayImpl::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderPhoto> findById(Integer orderId, Long photoId) {
        return photos.findByIdAndOrderId(photoId, orderId).map(OrderPhotoGatewayImpl::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByOrderId(Integer orderId) {
        return photos.countByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrderPhoto> findByStorageKey(String storageKey) {
        return photos.findByStorageKey(storageKey).map(OrderPhotoGatewayImpl::toDomain);
    }

    @Override
    @Transactional
    public void delete(Long photoId) {
        photos.deleteById(photoId);
    }

    private static OrderPhoto toDomain(OrderPhotoJpaEntity entity) {
        return new OrderPhoto(entity.getId(), entity.getOrderId(), entity.getKind(), entity.getStorageKey(),
                entity.getContentType(), entity.getSize(), entity.getCreatedAt());
    }
}
