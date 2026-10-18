package com.undercontroll.application.mapper;

import com.undercontroll.application.dto.order.CreateOrderRequest;
import com.undercontroll.application.dto.order.GetAllOrdersResponse;
import com.undercontroll.application.dto.order.OrderEnrichedDto;
import com.undercontroll.application.dto.order.PartDto;
import com.undercontroll.application.dto.order.UpdateOrderRequest;
import com.undercontroll.application.dto.orderitem.OrderItemCreateOrderRequest;
import com.undercontroll.application.dto.orderitem.UpdateOrderItemDto;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.enums.UserType;
import com.undercontroll.domain.usecase.component.ComponentOutput;
import com.undercontroll.domain.usecase.order.CreateOrderPort;
import com.undercontroll.domain.usecase.order.GetOrdersPort;
import com.undercontroll.domain.usecase.order.OrderOutput;
import com.undercontroll.domain.usecase.order.OrderPartInput;
import com.undercontroll.domain.usecase.order.UpdateOrderPort;
import com.undercontroll.domain.usecase.order_item.OrderItemOutput;
import com.undercontroll.domain.usecase.user.UserOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringJUnitConfig(classes = {
        OrderDtoMapperImpl.class,
        UserDtoMapperImpl.class,
        OrderItemDtoMapperImpl.class,
        ComponentPartDtoMapperImpl.class
})
class OrderDtoMapperTest {

    @Autowired
    private OrderDtoMapper mapper;

    private static OrderOutput order() {
        return new OrderOutput(
                7,
                new UserOutput(3, "Ana", "ana@x.com", "Souza", "Rua 1", "123", "01000-000", "1199", null, true, false, false, UserType.CUSTOMER),
                List.of(new OrderItemOutput(1, null, "X1", "TV", "LG", "220V", "SN1", 80.0, null)),
                List.of(new ComponentOutput(5, "Capacitor", "desc", "Epcos", 2.5, 10L, "Loja", "Elétrica")),
                2.5,
                80.0,
                0.0,
                82.5,
                "2026-01-10",
                null,
                "NF-1",
                true,
                "Não gela",
                null,
                OrderStatus.PENDING,
                null
        );
    }

    @Test
    @DisplayName("maps the order output tree to the enriched DTO")
    void toDto() {
        OrderEnrichedDto dto = mapper.toDto(order());

        assertEquals(7, dto.id());
        assertEquals("ana@x.com", dto.user().email());
        assertEquals("01000-000", dto.user().CEP());
        assertEquals("LG", dto.appliances().get(0).brand());
        assertEquals("Capacitor", dto.parts().get(0).item());
        assertEquals(82.5, dto.totalValue());
        assertEquals("Não gela", dto.customerDescription());
        assertNull(dto.technicalDescription());
        assertEquals(OrderStatus.PENDING, dto.status());
    }

    @Test
    @DisplayName("maps paginated orders")
    void toResponse() {
        GetAllOrdersResponse response = mapper.toResponse(new GetOrdersPort.Output(List.of(order()), 1L, 1, 0, 10));

        assertEquals(1, response.data().size());
        assertEquals(1L, response.totalElements());
        assertEquals(10, response.size());
    }

    @Test
    @DisplayName("maps create and update requests to port inputs")
    void toInput() {
        CreateOrderPort.Input create = mapper.toInput(new CreateOrderRequest(
                1,
                List.of(new OrderItemCreateOrderRequest("TV", "LG", "X1", "220V", "SN1", 50.0)),
                List.of(new PartDto(5, 2)),
                0.0, "10/01/2026", null, "Não gela", "Compressor", "PENDING", true, false, "NF-1"
        ));
        UpdateOrderPort.Input update = mapper.toInput(new UpdateOrderRequest(
                OrderStatus.COMPLETED,
                List.of(new UpdateOrderItemDto(9, "TV", "LG", "X1", "220V", "SN1", 70.0)),
                List.of(new PartDto(5, 0)),
                null,
                "Trocado"
        ));

        assertEquals(new CreateOrderPort.Appliance("TV", "LG", "X1", "220V", "SN1", 50.0), create.appliances().get(0));
        assertEquals(new OrderPartInput(5, 2), create.parts().get(0));
        assertEquals("NF-1", create.nf());
        assertEquals(new UpdateOrderPort.Appliance(9, "TV", "LG", "X1", "220V", "SN1", 70.0), update.appliances().get(0));
        assertEquals(OrderStatus.COMPLETED, update.status());
        assertEquals("Trocado", update.technicalDescription());
    }
}
