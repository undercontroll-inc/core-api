package com.undercontroll.application.mapper;

import com.undercontroll.application.dto.orderitem.CreateOrderItemRequest;
import com.undercontroll.application.dto.orderitem.OrderItemDto;
import com.undercontroll.application.dto.orderitem.UpdateOrderItemRequest;
import com.undercontroll.domain.usecase.order_item.CreateOrderItemPort;
import com.undercontroll.domain.usecase.order_item.OrderItemOutput;
import com.undercontroll.domain.usecase.order_item.UpdateOrderItemPort;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderItemDtoMapper {

    @Mapping(target = "voltage", source = "volt")
    @Mapping(target = "serial", source = "series")
    OrderItemDto toDto(OrderItemOutput output);

    @Mapping(target = "volt", source = "voltage")
    @Mapping(target = "series", source = "serial")
    CreateOrderItemPort.Input toInput(CreateOrderItemRequest request);

    @Mapping(target = "volt", source = "voltage")
    @Mapping(target = "series", source = "serial")
    UpdateOrderItemPort.Input toInput(UpdateOrderItemRequest request);
}
