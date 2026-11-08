package com.undercontroll.application.mapper;

import com.undercontroll.application.dto.order.CreateOrderRequest;
import com.undercontroll.application.dto.order.GetAllOrdersResponse;
import com.undercontroll.application.dto.order.OrderEnrichedDto;
import com.undercontroll.application.dto.order.UpdateOrderRequest;
import com.undercontroll.domain.usecase.order.CreateOrderPort;
import com.undercontroll.domain.usecase.order.GetOrdersPort;
import com.undercontroll.domain.usecase.order.OrderOutput;
import com.undercontroll.domain.usecase.order.UpdateOrderPort;
import com.undercontroll.application.dto.order.LabelReadingResponse;
import com.undercontroll.application.dto.order.OrderPhotoDto;
import com.undercontroll.application.dto.order.OrderStatusEventDto;
import com.undercontroll.application.dto.order.PublicOrderDto;
import com.undercontroll.application.dto.order.SendBudgetResponse;
import com.undercontroll.application.dto.order.TrackingLinkResponse;
import com.undercontroll.application.dto.orderitem.UpdateOrderItemDto;
import com.undercontroll.domain.model.LabelReading;
import com.undercontroll.domain.model.OrderStatusEvent;
import com.undercontroll.domain.usecase.order.CreateTrackingLinkPort;
import com.undercontroll.domain.usecase.order.OrderPhotoOutput;
import com.undercontroll.domain.usecase.order.PublicOrderView;
import com.undercontroll.domain.usecase.order.SendOrderBudgetPort;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {UserDtoMapper.class, OrderItemDtoMapper.class, ComponentPartDtoMapper.class}
)
public interface OrderDtoMapper {

    OrderEnrichedDto toDto(OrderOutput output);

    GetAllOrdersResponse toResponse(GetOrdersPort.Output output);

    CreateOrderPort.Input toInput(CreateOrderRequest request);

    UpdateOrderPort.Input toInput(UpdateOrderRequest request);

    @Mapping(target = "volt", source = "voltage")
    @Mapping(target = "series", source = "serial")
    UpdateOrderPort.Appliance toAppliance(UpdateOrderItemDto dto);

    SendBudgetResponse toResponse(SendOrderBudgetPort.Output output);

    TrackingLinkResponse toResponse(CreateTrackingLinkPort.Output output);

    OrderPhotoDto toDto(OrderPhotoOutput output);

    OrderStatusEventDto toDto(OrderStatusEvent event);

    PublicOrderDto toDto(PublicOrderView view);

    default LabelReadingResponse toResponse(LabelReading reading) {
        return new LabelReadingResponse(reading.isLabel(), reading.type(), reading.brand(), reading.model(),
                reading.serial(), reading.voltage(), reading.confidence());
    }
}
