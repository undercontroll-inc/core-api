package com.undercontroll.domain.usecase.order;

public interface DeleteOrderPhotoPort {

    void execute(Integer orderId, Long photoId);
}
