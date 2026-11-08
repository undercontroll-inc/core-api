package com.undercontroll.application.controller.impl;

import com.undercontroll.application.controller.OrderLifecycleApi;
import com.undercontroll.application.dto.order.BudgetDecisionRequest;
import com.undercontroll.application.dto.order.LabelReadingResponse;
import com.undercontroll.application.dto.order.OrderEnrichedDto;
import com.undercontroll.application.dto.order.OrderLookupResponse;
import com.undercontroll.application.dto.order.OrderPhotoDto;
import com.undercontroll.application.dto.order.SendBudgetResponse;
import com.undercontroll.application.dto.order.TrackingLinkResponse;
import com.undercontroll.application.dto.order.TransitionOrderRequest;
import com.undercontroll.application.dto.order.PhotoUploadRequest;
import com.undercontroll.application.dto.storage.UploadReference;
import com.undercontroll.application.dto.storage.UploadRequest;
import com.undercontroll.application.dto.storage.UploadTicketDto;
import com.undercontroll.application.mapper.OrderDtoMapper;
import com.undercontroll.application.mapper.UploadDtoMapper;
import com.undercontroll.domain.usecase.order.CompleteOrderPhotoUploadPort;
import com.undercontroll.domain.usecase.order.RequestLabelUploadPort;
import com.undercontroll.domain.usecase.order.RequestOrderPhotoUploadPort;
import com.undercontroll.domain.usecase.order.CreateTrackingLinkPort;
import com.undercontroll.domain.usecase.order.DecideOrderBudgetPort;
import com.undercontroll.domain.usecase.order.DeleteOrderPhotoPort;
import com.undercontroll.domain.usecase.order.ListOrderPhotosPort;
import com.undercontroll.domain.usecase.order.LookupOrderByTokenPort;
import com.undercontroll.domain.usecase.order.ReadApplianceLabelPort;
import com.undercontroll.domain.usecase.order.SendOrderBudgetPort;
import com.undercontroll.domain.usecase.order.TransitionOrderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OrderLifecycleController implements OrderLifecycleApi {

    private final TransitionOrderPort transitionOrderPort;
    private final SendOrderBudgetPort sendOrderBudgetPort;
    private final DecideOrderBudgetPort decideOrderBudgetPort;
    private final CreateTrackingLinkPort createTrackingLinkPort;
    private final LookupOrderByTokenPort lookupOrderByTokenPort;
    private final ReadApplianceLabelPort readApplianceLabelPort;
    private final RequestOrderPhotoUploadPort requestOrderPhotoUploadPort;
    private final CompleteOrderPhotoUploadPort completeOrderPhotoUploadPort;
    private final RequestLabelUploadPort requestLabelUploadPort;
    private final UploadDtoMapper uploadMapper;
    private final ListOrderPhotosPort listOrderPhotosPort;
    private final DeleteOrderPhotoPort deleteOrderPhotoPort;
    private final OrderDtoMapper mapper;

    @Override
    public ResponseEntity<OrderEnrichedDto> transition(Integer orderId, TransitionOrderRequest request) {
        TransitionOrderRequest body = request == null ? new TransitionOrderRequest(null, null, null) : request;
        return ResponseEntity.ok(mapper.toDto(transitionOrderPort.execute(
                new TransitionOrderPort.Input(orderId, body.to(), body.outcome(), body.note()))));
    }

    @Override
    public ResponseEntity<SendBudgetResponse> sendBudget(Integer orderId) {
        return ResponseEntity.ok(mapper.toResponse(sendOrderBudgetPort.execute(orderId)));
    }

    @Override
    public ResponseEntity<OrderEnrichedDto> decideBudget(Integer orderId, BudgetDecisionRequest request) {
        BudgetDecisionRequest body = request == null ? new BudgetDecisionRequest(null, null) : request;
        return ResponseEntity.ok(mapper.toDto(decideOrderBudgetPort.execute(
                new DecideOrderBudgetPort.Input(orderId, body.decision(), body.note()))));
    }

    @Override
    public ResponseEntity<TrackingLinkResponse> trackingLink(Integer orderId) {
        return ResponseEntity.ok(mapper.toResponse(createTrackingLinkPort.execute(orderId)));
    }

    @Override
    public ResponseEntity<OrderLookupResponse> lookup(String token) {
        return ResponseEntity.ok(new OrderLookupResponse(lookupOrderByTokenPort.execute(token)));
    }

    @Override
    public ResponseEntity<UploadTicketDto> requestLabelUpload(UploadRequest request) {
        UploadRequest body = request == null ? new UploadRequest(null, null, null) : request;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(uploadMapper.toDto(requestLabelUploadPort.execute(body.contentType(), body.sizeBytes())));
    }

    @Override
    public ResponseEntity<LabelReadingResponse> readLabel(UploadReference request) {
        return ResponseEntity.ok(mapper.toResponse(readApplianceLabelPort.execute(request == null ? null : request.uploadId())));
    }

    @Override
    public ResponseEntity<UploadTicketDto> requestPhotoUpload(Integer orderId, PhotoUploadRequest request) {
        PhotoUploadRequest body = request == null ? new PhotoUploadRequest(null, null, null) : request;
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadMapper.toDto(requestOrderPhotoUploadPort.execute(
                new RequestOrderPhotoUploadPort.Input(orderId, body.kind(), body.contentType(), body.sizeBytes()))));
    }

    @Override
    public ResponseEntity<OrderPhotoDto> completePhotoUpload(Integer orderId, UUID uploadId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toDto(completeOrderPhotoUploadPort.execute(orderId, uploadId)));
    }

    @Override
    public ResponseEntity<List<OrderPhotoDto>> listPhotos(Integer orderId) {
        return ResponseEntity.ok(listOrderPhotosPort.execute(orderId).stream().map(mapper::toDto).toList());
    }

    @Override
    public ResponseEntity<Void> deletePhoto(Integer orderId, Long photoId) {
        deleteOrderPhotoPort.execute(orderId, photoId);
        return ResponseEntity.noContent().build();
    }
}
