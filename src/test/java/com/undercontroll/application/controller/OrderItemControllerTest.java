package com.undercontroll.application.controller;

import com.undercontroll.application.controller.impl.OrderItemController;
import com.undercontroll.application.mapper.OrderItemDtoMapperImpl;
import com.undercontroll.domain.gateway.TokenGateway;
import com.undercontroll.domain.usecase.order_item.CreateOrderItemPort;
import com.undercontroll.domain.usecase.order_item.DeleteOrderItemPort;
import com.undercontroll.domain.usecase.order_item.GetOrderItemByIdPort;
import com.undercontroll.domain.usecase.order_item.GetOrderItemsPort;
import com.undercontroll.domain.usecase.order_item.OrderItemOutput;
import com.undercontroll.domain.usecase.order_item.UpdateOrderItemPort;
import com.undercontroll.infrastructure.config.RateLimitProperties;
import com.undercontroll.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderItemController.class)
@Import({SecurityConfig.class, RateLimitProperties.class, OrderItemDtoMapperImpl.class})
class OrderItemControllerTest {

    private static final OrderItemOutput ITEM = new OrderItemOutput(10, null, "X1", "TV", "LG", "220V", "SN1", 80.0, null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateOrderItemPort createOrderItemPort;
    @MockitoBean
    private UpdateOrderItemPort updateOrderItemPort;
    @MockitoBean
    private GetOrderItemsPort getOrderItemsPort;
    @MockitoBean
    private GetOrderItemByIdPort getOrderItemByIdPort;
    @MockitoBean
    private DeleteOrderItemPort deleteOrderItemPort;
    @MockitoBean
    private TokenGateway tokenGateway;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("creates and updates items mapping the request bodies")
    void createAndUpdate() throws Exception {
        when(createOrderItemPort.execute(1, new CreateOrderItemPort.Input("LG", "X1", "TV", null, "220V", "SN1", 80.0)))
                .thenReturn(ITEM);

        mockMvc.perform(post("/v1/api/orders/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"brand\":\"LG\",\"model\":\"X1\",\"type\":\"TV\",\"volt\":\"220V\",\"series\":\"SN1\",\"laborValue\":80.0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.laborValue").value(80.0));

        mockMvc.perform(patch("/v1/api/orders/1/items/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"labor\":90.0}"))
                .andExpect(status().isOk());

        verify(updateOrderItemPort).execute(1, 10, new UpdateOrderItemPort.Input(null, 90.0, null, null, null, null, null, null));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("reads, misses and deletes items")
    void readAndDelete() throws Exception {
        when(getOrderItemsPort.execute(1)).thenReturn(List.of(ITEM));
        when(getOrderItemByIdPort.execute(1, 10)).thenReturn(Optional.of(ITEM));
        when(getOrderItemByIdPort.execute(1, 11)).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/api/orders/1/items")).andExpect(jsonPath("$[0].brand").value("LG"));
        mockMvc.perform(get("/v1/api/orders/1/items/10")).andExpect(jsonPath("$.series").value("SN1"));
        mockMvc.perform(get("/v1/api/orders/1/items/11")).andExpect(status().isNotFound());
        mockMvc.perform(delete("/v1/api/orders/1/items/10")).andExpect(status().isNoContent());

        verify(deleteOrderItemPort).execute(1, 10);
    }
}
