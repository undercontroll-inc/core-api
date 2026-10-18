package com.undercontroll.application.controller;

import com.undercontroll.application.mapper.OrderDtoMapperImpl;
import com.undercontroll.application.mapper.UserDtoMapperImpl;
import com.undercontroll.application.mapper.OrderItemDtoMapperImpl;
import com.undercontroll.application.mapper.ComponentPartDtoMapperImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.application.dto.order.CreateOrderRequest;
import com.undercontroll.application.dto.order.UpdateOrderRequest;
import com.undercontroll.domain.usecase.user.UserOutput;
import com.undercontroll.domain.exception.UnauthorizedOrderOperation;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.enums.UserType;
import com.undercontroll.domain.gateway.TokenGateway;
import com.undercontroll.domain.usecase.order.*;
import com.undercontroll.infrastructure.config.SecurityConfig;
import com.undercontroll.application.controller.impl.OrderController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({SecurityConfig.class, OrderDtoMapperImpl.class, UserDtoMapperImpl.class, OrderItemDtoMapperImpl.class, ComponentPartDtoMapperImpl.class})
@AutoConfigureMockMvc(addFilters = true)
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateOrderPort createOrderPort;

    @MockitoBean
    private UpdateOrderPort updateOrderPort;

    @MockitoBean
    private GetOrdersPort getOrdersPort;

    @MockitoBean
    private GetOrderByIdPort getOrderByIdPort;

    @MockitoBean
    private DeleteOrderPort deleteOrderPort;

    @MockitoBean
    private ExportOrderPort exportOrderPort;

    // Required because AuthContextFilter depends on TokenPort
    @MockitoBean
    private TokenGateway tokenGateway;

    @Test
    @DisplayName("POST /v1/api/orders - ADMINISTRATOR should create order and return 201")
    void administratorShouldCreateOrderSuccessfully() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                1, List.of(), List.of(), 0.0, "20/11/2025", "25/11/2025",
                "Não gela", "Compressor queimado", "PENDING", false, true, "NF123"
        );

        OrderOutput response = new OrderOutput(
                1, null, List.of(), List.of(), 0.0, 0.0, 0.0, 0.0,
                "20/11/2025", "25/11/2025", "NF123", false, "Não gela",
                "Compressor queimado", OrderStatus.PENDING, null
        );

        when(createOrderPort.execute(any(CreateOrderPort.Input.class))).thenReturn(response);

        mockMvc.perform(post("/v1/api/orders")
                        .with(user("admin@example.com").roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(createOrderPort, times(1)).execute(any(CreateOrderPort.Input.class));
    }

    @Test
    @DisplayName("POST /v1/api/orders - CUSTOMER should create order and return 201")
    void customerShouldCreateOrderSuccessfully() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                1, List.of(), List.of(), 0.0, "20/11/2025", "25/11/2025",
                "Não gela", "Compressor queimado", "PENDING", false, true, "NF123"
        );

        OrderOutput response = new OrderOutput(
                1, null, List.of(), List.of(), 0.0, 0.0, 0.0, 0.0,
                "20/11/2025", "25/11/2025", "NF123", false, "Não gela",
                "Compressor queimado", OrderStatus.PENDING, null
        );

        when(createOrderPort.execute(any(CreateOrderPort.Input.class))).thenReturn(response);

        mockMvc.perform(post("/v1/api/orders")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(createOrderPort, times(1)).execute(any(CreateOrderPort.Input.class));
    }

    @Test
    @DisplayName("PATCH /v1/api/orders/{orderId} - ADMINISTRATOR should update order and return 200")
    void administratorShouldUpdateOrderSuccessfully() throws Exception {
        UpdateOrderRequest request = new UpdateOrderRequest(
                OrderStatus.COMPLETED, List.of(), List.of(), "Reparo em analise", "Aguardando peca"
        );

        mockMvc.perform(patch("/v1/api/orders/1")
                        .with(user("admin@example.com").roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(updateOrderPort, times(1)).execute(eq(1), any(UpdateOrderPort.Input.class));
    }

    @Test
    @DisplayName("PATCH /v1/api/orders/{orderId} - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToUpdateOrder() throws Exception {
        UpdateOrderRequest request = new UpdateOrderRequest(
                OrderStatus.COMPLETED, List.of(), List.of(), "Reparo em analise", "Aguardando peca"
        );

        mockMvc.perform(patch("/v1/api/orders/1")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(updateOrderPort, never()).execute(any(), any());
    }

    @Test
    @DisplayName("GET /v1/api/orders - ADMINISTRATOR should get all orders and return 200")
    void administratorShouldGetAllOrdersSuccessfully() throws Exception {
        UserOutput userDto = new UserOutput(1, "John", "john@example.com", "Doe",
                "Street 123", "12345678900", "12345-678", "11999999999",
                null, false, false, true, UserType.CUSTOMER);

        OrderOutput order = new OrderOutput(
                1, userDto, List.of(), List.of(), 100.0, 50.0, 10.0, 140.0,
                "20/11/2025", "25/11/2025", "NF123", true, "Não gela",
                "Compressor queimado", OrderStatus.PENDING, "20/11/2025"
        );

        when(getOrdersPort.execute(isNull(), anyInt(), anyInt()))
                .thenReturn(new GetOrdersPort.Output(List.of(order), 1L, 1, 0, 10));

        mockMvc.perform(get("/v1/api/orders")
                        .with(user("admin@example.com").roles("ADMINISTRATOR"))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].customerDescription").value("Não gela"))
                .andExpect(jsonPath("$.data[0].technicalDescription").value("Compressor queimado"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(getOrdersPort, times(1)).execute(isNull(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("GET /v1/api/orders - CUSTOMER without userId should be forbidden and return 403")
    void customerShouldBeForbiddenToGetAllOrders() throws Exception {
        when(getOrdersPort.execute(isNull(), anyInt(), anyInt()))
                .thenThrow(new UnauthorizedOrderOperation("Customers can only list their own orders"));

        mockMvc.perform(get("/v1/api/orders")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(UnauthorizedOrderOperation.CODE));
    }

    @Test
    @DisplayName("GET /v1/api/orders/{orderId} - CUSTOMER should get order by id and return 200")
    void customerShouldGetOrderByIdSuccessfully() throws Exception {
        UserOutput userDto = new UserOutput(1, "John", "john@example.com", "Doe",
                "Street 123", "12345678900", "12345-678", "11999999999",
                null, false, false, true, UserType.CUSTOMER);

        OrderOutput enrichedDto = new OrderOutput(
                1, userDto, List.of(), List.of(), 100.0, 50.0, 10.0, 140.0,
                "20/11/2025", "25/11/2025", "NF123", true, "Não gela",
                null, OrderStatus.PENDING, "20/11/2025"
        );

        when(getOrderByIdPort.execute(1)).thenReturn(Optional.of(enrichedDto));

        mockMvc.perform(get("/v1/api/orders/1")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.customerDescription").value("Não gela"))
                .andExpect(jsonPath("$.data.technicalDescription").value(nullValue()));

        verify(getOrderByIdPort, times(1)).execute(1);
    }

    @Test
    @DisplayName("DELETE /v1/api/orders/{orderId} - ADMINISTRATOR should delete order and return 204")
    void administratorShouldDeleteOrderSuccessfully() throws Exception {
        mockMvc.perform(delete("/v1/api/orders/1")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isNoContent());

        verify(deleteOrderPort, times(1)).execute(1);
    }

    @Test
    @DisplayName("DELETE /v1/api/orders/{orderId} - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToDeleteOrder() throws Exception {
        mockMvc.perform(delete("/v1/api/orders/1")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isForbidden());

        verify(deleteOrderPort, never()).execute(any());
    }

    @Test
    @DisplayName("GET /v1/api/orders?userId=X - CUSTOMER should get their own orders and return 200")
    void customerShouldGetOrdersByUserIdSuccessfully() throws Exception {
        UserOutput userDto = new UserOutput(1, "John", "john@example.com", "Doe",
                "Street 123", "12345678900", "12345-678", "11999999999",
                null, false, false, true, UserType.CUSTOMER);

        OrderOutput order = new OrderOutput(
                1, userDto, List.of(), List.of(), 100.0, 50.0, 10.0, 140.0,
                "20/11/2025", "25/11/2025", "NF123", true, "Não gela",
                null, OrderStatus.PENDING, "20/11/2025"
        );

        when(getOrdersPort.execute(eq(1), anyInt(), anyInt()))
                .thenReturn(new GetOrdersPort.Output(List.of(order), 1L, 1, 0, 1));

        mockMvc.perform(get("/v1/api/orders")
                        .with(user("1").roles("CUSTOMER"))
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].user.id").value(1));

        verify(getOrdersPort, times(1)).execute(eq(1), anyInt(), anyInt());
    }

    @Test
    @DisplayName("GET /v1/api/orders?userId=X - CUSTOMER requesting another user's orders should be forbidden")
    void customerShouldBeForbiddenToGetAnotherUsersOrders() throws Exception {
        when(getOrdersPort.execute(eq(2), anyInt(), anyInt()))
                .thenThrow(new UnauthorizedOrderOperation("Customers can only list their own orders"));

        mockMvc.perform(get("/v1/api/orders")
                        .with(user("1").roles("CUSTOMER"))
                        .param("userId", "2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /v1/api/orders?userId=X - ADMINISTRATOR should get orders by userId and return 200")
    void administratorShouldGetOrdersByUserIdSuccessfully() throws Exception {
        UserOutput userDto = new UserOutput(1, "John", "john@example.com", "Doe",
                "Street 123", "12345678900", "12345-678", "11999999999",
                null, false, false, true, UserType.CUSTOMER);

        OrderOutput order = new OrderOutput(
                1, userDto, List.of(), List.of(), 100.0, 50.0, 10.0, 140.0,
                "20/11/2025", "25/11/2025", "NF123", true, "Não gela",
                null, OrderStatus.PENDING, "20/11/2025"
        );

        when(getOrdersPort.execute(eq(1), anyInt(), anyInt()))
                .thenReturn(new GetOrdersPort.Output(List.of(order), 1L, 1, 0, 1));

        mockMvc.perform(get("/v1/api/orders")
                        .with(user("admin@example.com").roles("ADMINISTRATOR"))
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(getOrdersPort, times(1)).execute(eq(1), anyInt(), anyInt());
    }

    @Test
    @DisplayName("GET /v1/api/orders/{orderId}/export - CUSTOMER should export order as PDF and return 200")
    void customerShouldExportOrderSuccessfully() throws Exception {
        byte[] pdf = "pdf-bytes".getBytes();

        when(exportOrderPort.execute(1)).thenReturn(pdf);

        mockMvc.perform(get("/v1/api/orders/1/export")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk());

        verify(exportOrderPort, times(1)).execute(1);
    }
}
