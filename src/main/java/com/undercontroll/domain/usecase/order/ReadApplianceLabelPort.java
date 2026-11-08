package com.undercontroll.domain.usecase.order;

import com.undercontroll.domain.model.LabelReading;

import java.util.UUID;

public interface ReadApplianceLabelPort {

    LabelReading execute(UUID uploadId);
}
