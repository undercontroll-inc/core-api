package com.undercontroll.domain.usecase.order;

import java.util.UUID;

public interface CompleteOrderPhotoUploadPort {

    OrderPhotoOutput execute(Integer orderId, UUID uploadId);
}
