package com.undercontroll.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.application.dto.*;
import com.undercontroll.domain.model.enums.OrderStatus;
import com.undercontroll.domain.model.enums.UserType;
import com.undercontroll.domain.port.in.*;
import com.undercontroll.domain.port.out.TokenPort;
import com.undercontroll.infrastructure.config.SecurityConfig;
import com.undercontroll.infrastructure.config.RateLimitProperties;
import com.undercontroll.infrastructure.web.dto.CreateOrderRequest;
import com.undercontroll.infrastructure.web.dto.GetAllOrdersResponse;
import com.undercontroll.infrastructure.web.dto.UpdateOrderRequest;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({SecurityConfig.class, RateLimitProperties.class})
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
    private GetOrdersByUserIdPort getOrdersByUserIdPort;

    @MockitoBean
    private ExportOrderPort exportOrderPort;

    // Required because AuthContextFilter depends on TokenPort
    @MockitoBean
    private TokenPort tokenPort;

    @Test
    @DisplayName("POST /v1/api/orders - ADMINISTRATOR should create order and return 201")
    void administratorShouldCreateOrderSuccessfully() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                1, List.of(), List.of(), 0.0, "20/11/2025", "25/11/2025",
                "Service description", "Notes", "PENDING", false, true, "NF123"
        );

        when(createOrderPort.execute(any(CreateOrderPort.Input.class)))
                .thenReturn(new CreateOrderPort.Output(1, 1, "PENDING", 0.0));

        mockMvc.perform(post("/v1/api/orders")
                        .with(user("admin@example.com").roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(createOrderPort, times(1)).execute(any(CreateOrderPort.Input.class));
    }

    @Test
    @DisplayName("POST /v1/api/orders - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToCreateOrder() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                1, List.of(), List.of(), 0.0, "20/11/2025", "25/11/2025",
                "Service description", "Notes", "PENDING", false, true, "NF123"
        );

        mockMvc.perform(post("/v1/api/orders")
                        .with(user("customer@example.com").roles("SCOPE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(createOrderPort, never()).execute(any(CreateOrderPort.Input.class));
    }

    @Test
    @DisplayName("PATCH /v1/api/orders/{id} - ADMINISTRATOR should update order and return 200")
    void administratorShouldUpdateOrderSuccessfully() throws Exception {
        UpdateOrderRequest request = new UpdateOrderRequest(
                OrderStatus.COMPLETED, List.of(), List.of(), "Updated description"
        );

        when(updateOrderPort.execute(any(UpdateOrderPort.Input.class)))
                .thenReturn(new UpdateOrderPort.Output(true, "OK"));

        mockMvc.perform(patch("/v1/api/orders/1")
                        .with(user("admin@example.com").roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(updateOrderPort, times(1)).execute(any(UpdateOrderPort.Input.class));
    }

    @Test
    @DisplayName("GET /v1/api/orders - ADMINISTRATOR should get all orders and return 200")
    void administratorShouldGetAllOrdersSuccessfully() throws Exception {
        UserDto userDto = new UserDto(1, "John", "john@example.com", "Doe",
                "Street 123", "12345678900", "12345-678", "11999999999",
                null, false, false, true, UserType.CUSTOMER);

        OrderEnrichedDto order = new OrderEnrichedDto(
                1, userDto, List.of(), List.of(), 100.0, 50.0, 10.0, 140.0,
                "20/11/2025", "25/11/2025", "NF123", true, "Description",
                null, OrderStatus.PENDING, "20/11/2025"
        );

        when(getOrdersPort.execute(any(GetOrdersPort.Input.class)))
                .thenReturn(new GetOrdersPort.Output(List.of(order)));

        mockMvc.perform(get("/v1/api/orders")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));

        verify(getOrdersPort, times(1)).execute(any(GetOrdersPort.Input.class));
    }

    @Test
    @DisplayName("GET /v1/api/orders - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToGetAllOrders() throws Exception {
        mockMvc.perform(get("/v1/api/orders")
                        .with(user("customer@example.com").roles("SCOPE_CUSTOMER")))
                .andExpect(status().isForbidden());

        verify(getOrdersPort, never()).execute(any(GetOrdersPort.Input.class));
    }

    @Test
    @DisplayName("GET /v1/api/orders/{orderId} - CUSTOMER should get order by id and return 200")
    void customerShouldGetOrderByIdSuccessfully() throws Exception {
        UserDto userDto = new UserDto(1, "John", "john@example.com", "Doe",
                "Street 123", "12345678900", "12345-678", "11999999999",
                null, false, false, true, UserType.CUSTOMER);

        OrderEnrichedDto enrichedDto = new OrderEnrichedDto(
                1, userDto, List.of(), List.of(), 100.0, 50.0, 10.0, 140.0,
                "20/11/2025", "25/11/2025", "NF123", true, "Description",
                null, OrderStatus.PENDING, "20/11/2025"
        );

        GetOrderByIdResponse response = new GetOrderByIdResponse(enrichedDto);

        when(getOrderByIdPort.execute(any(GetOrderByIdPort.Input.class)))
                .thenReturn(new GetOrderByIdPort.Output(response));

        mockMvc.perform(get("/v1/api/orders/1")
                        .with(user("customer@example.com").roles("SCOPE_CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(getOrderByIdPort, times(1)).execute(any(GetOrderByIdPort.Input.class));
    }

    @Test
    @DisplayName("DELETE /v1/api/orders/{orderId} - ADMINISTRATOR should delete order and return 204")
    void administratorShouldDeleteOrderSuccessfully() throws Exception {
        when(deleteOrderPort.execute(any(DeleteOrderPort.Input.class)))
                .thenReturn(new DeleteOrderPort.Output(true, "Deleted"));

        mockMvc.perform(delete("/v1/api/orders/1")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isNoContent());

        verify(deleteOrderPort, times(1)).execute(any(DeleteOrderPort.Input.class));
    }

    @Test
    @DisplayName("DELETE /v1/api/orders/{orderId} - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToDeleteOrder() throws Exception {
        mockMvc.perform(delete("/v1/api/orders/1")
                        .with(user("customer@example.com").roles("SCOPE_CUSTOMER")))
                .andExpect(status().isForbidden());

        verify(deleteOrderPort, never()).execute(any(DeleteOrderPort.Input.class));
    }

    @Test
    @DisplayName("GET /v1/api/orders/filter?userId=X - CUSTOMER should get orders by userId and return 200")
    void customerShouldGetOrdersByUserIdSuccessfully() throws Exception {
        UserDto userDto = new UserDto(1, "John", "john@example.com", "Doe",
                "Street 123", "12345678900", "12345-678", "11999999999",
                null, false, false, true, UserType.CUSTOMER);

        OrderEnrichedDto order = new OrderEnrichedDto(
                1, userDto, List.of(), List.of(), 100.0, 50.0, 10.0, 140.0,
                "20/11/2025", "25/11/2025", "NF123", true, "Description",
                null, OrderStatus.PENDING, "20/11/2025"
        );

        GetOrdersByUserIdResponse response = new GetOrdersByUserIdResponse(List.of(order));

        when(getOrdersByUserIdPort.execute(any(GetOrdersByUserIdPort.Input.class)))
                .thenReturn(new GetOrdersByUserIdPort.Output(response));

        mockMvc.perform(get("/v1/api/orders/filter")
                        .with(user("1").roles("SCOPE_CUSTOMER"))
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].user.id").value(1));

        verify(getOrdersByUserIdPort, times(1)).execute(any(GetOrdersByUserIdPort.Input.class));
    }

    @Test
    @DisplayName("GET /v1/api/orders/filter?userId=X - ADMINISTRATOR should get orders by userId and return 200")
    void administratorShouldGetOrdersByUserIdSuccessfully() throws Exception {
        UserDto userDto = new UserDto(1, "John", "john@example.com", "Doe",
                "Street 123", "12345678900", "12345-678", "11999999999",
                null, false, false, true, UserType.CUSTOMER);

        OrderEnrichedDto order = new OrderEnrichedDto(
                1, userDto, List.of(), List.of(), 100.0, 50.0, 10.0, 140.0,
                "20/11/2025", "25/11/2025", "NF123", true, "Description",
                null, OrderStatus.PENDING, "20/11/2025"
        );

        GetOrdersByUserIdResponse response = new GetOrdersByUserIdResponse(List.of(order));

        when(getOrdersByUserIdPort.execute(any(GetOrdersByUserIdPort.Input.class)))
                .thenReturn(new GetOrdersByUserIdPort.Output(response));

        mockMvc.perform(get("/v1/api/orders/filter")
                        .with(user("1").roles("ADMINISTRATOR", "SCOPE_ADMINISTRATOR"))
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(getOrdersByUserIdPort, times(1)).execute(any(GetOrdersByUserIdPort.Input.class));
    }
}
