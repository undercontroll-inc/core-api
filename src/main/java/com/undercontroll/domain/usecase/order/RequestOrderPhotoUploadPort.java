package com.undercontroll.domain.usecase.order;

import com.undercontroll.domain.enums.OrderPhotoKind;
import com.undercontroll.domain.storage.UploadTicket;

public interface RequestOrderPhotoUploadPort {

    UploadTicket execute(Input input);

    record Input(Integer orderId, OrderPhotoKind kind, String contentType, Long sizeBytes) {
    }
}
