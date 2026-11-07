package com.undercontroll.application.mapper;

import com.undercontroll.application.dto.orderitem.CreateOrderItemRequest;
import com.undercontroll.application.dto.orderitem.OrderItemDto;
import com.undercontroll.application.dto.orderitem.UpdateOrderItemRequest;
import com.undercontroll.domain.usecase.order_item.CreateOrderItemPort;
import com.undercontroll.domain.usecase.order_item.OrderItemOutput;
import com.undercontroll.domain.usecase.order_item.UpdateOrderItemPort;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderItemDtoMapper {

    OrderItemDto toDto(OrderItemOutput output);

    CreateOrderItemPort.Input toInput(CreateOrderItemRequest request);

    UpdateOrderItemPort.Input toInput(UpdateOrderItemRequest request);
}
