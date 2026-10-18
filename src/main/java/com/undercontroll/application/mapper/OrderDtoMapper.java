package com.undercontroll.application.mapper;

import com.undercontroll.application.dto.order.CreateOrderRequest;
import com.undercontroll.application.dto.order.GetAllOrdersResponse;
import com.undercontroll.application.dto.order.OrderEnrichedDto;
import com.undercontroll.application.dto.order.UpdateOrderRequest;
import com.undercontroll.domain.usecase.order.CreateOrderPort;
import com.undercontroll.domain.usecase.order.GetOrdersPort;
import com.undercontroll.domain.usecase.order.OrderOutput;
import com.undercontroll.domain.usecase.order.UpdateOrderPort;
import org.mapstruct.Mapper;
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
}
