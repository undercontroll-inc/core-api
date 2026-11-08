package com.undercontroll.domain.usecase.order;

import com.undercontroll.domain.storage.UploadTicket;

public interface RequestLabelUploadPort {

    UploadTicket execute(String contentType, Long sizeBytes);
}
