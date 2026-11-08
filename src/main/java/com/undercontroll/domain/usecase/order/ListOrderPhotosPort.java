package com.undercontroll.domain.usecase.order;

import java.util.List;

public interface ListOrderPhotosPort {

    List<OrderPhotoOutput> execute(Integer orderId);
}
