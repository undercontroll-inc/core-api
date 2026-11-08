package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.enums.OrderPhotoKind;
import com.undercontroll.domain.exception.InvalidOrderPhotoException;
import com.undercontroll.domain.exception.OrderPhotoLimitException;
import com.undercontroll.domain.exception.OrderPhotoNotFoundException;
import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.gateway.OrderPhotoGateway;
import com.undercontroll.domain.gateway.UploadIntentGateway;
import com.undercontroll.domain.model.OrderPhoto;
import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.storage.UploadIntent;
import com.undercontroll.domain.storage.UploadTicket;
import com.undercontroll.domain.usecase.order.CompleteOrderPhotoUploadPort;
import com.undercontroll.domain.usecase.order.DeleteOrderPhotoPort;
import com.undercontroll.domain.usecase.order.ListOrderPhotosPort;
import com.undercontroll.domain.usecase.order.OrderPhotoOutput;
import com.undercontroll.domain.usecase.order.RequestOrderPhotoUploadPort;
import com.undercontroll.domain.usecase.storage.StorageUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPhotoUseCases implements RequestOrderPhotoUploadPort, CompleteOrderPhotoUploadPort,
        ListOrderPhotosPort, DeleteOrderPhotoPort {

    public static final int MAX_PHOTOS = 6;

    private final OrderAccess orderAccess;
    private final OrderPhotoGateway photos;
    private final UploadIntentGateway intents;
    private final StorageUploadService uploads;
    private final ObjectStorageGateway storage;
    private final OrderDetailsAssembler assembler;
    private final Clock clock;

    @Override
    public UploadTicket execute(RequestOrderPhotoUploadPort.Input input) {
        orderAccess.require(input.orderId());
        if (input.kind() == null) {
            throw new InvalidOrderPhotoException("Informe o tipo da foto: LABEL, DEVICE ou DAMAGE");
        }
        String reference = String.valueOf(input.orderId());
        long used = photos.countByOrderId(input.orderId())
                + intents.countPending(StorageArea.ORDER_PHOTOS, reference, clock.instant());
        if (used >= MAX_PHOTOS) {
            throw new OrderPhotoLimitException(MAX_PHOTOS);
        }
        return uploads.request(StorageArea.ORDER_PHOTOS, input.contentType(), input.sizeBytes(), null,
                reference, input.kind().name());
    }

    @Override
    public OrderPhotoOutput execute(Integer orderId, UUID uploadId) {
        orderAccess.require(orderId);
        UploadIntent upload = uploads.complete(uploadId, StorageArea.ORDER_PHOTOS, String.valueOf(orderId));
        OrderPhoto photo = photos.findByStorageKey(upload.objectKey()).orElseGet(() -> {
            if (photos.countByOrderId(orderId) >= MAX_PHOTOS) {
                throw new OrderPhotoLimitException(MAX_PHOTOS);
            }
            OrderPhoto saved = photos.save(new OrderPhoto(null, orderId, OrderPhotoKind.valueOf(upload.kind()),
                    upload.objectKey(), upload.contentType(), upload.sizeBytes(), clock.instant()));
            log.info("Order photo added orderId={} photoId={} kind={}", orderId, saved.id(), saved.kind());
            return saved;
        });
        return assembler.photo(photo);
    }

    @Override
    public List<OrderPhotoOutput> execute(Integer orderId) {
        orderAccess.require(orderId);
        return assembler.photos(orderId);
    }

    @Override
    public void execute(Integer orderId, Long photoId) {
        OrderPhoto photo = photos.findById(orderId, photoId).orElseThrow(() -> new OrderPhotoNotFoundException(photoId));
        photos.delete(photo.id());
        storage.delete(StorageArea.ORDER_PHOTOS, photo.storageKey());
    }
}
