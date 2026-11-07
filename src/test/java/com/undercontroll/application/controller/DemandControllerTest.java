package com.undercontroll.application.controller;

import com.undercontroll.application.controller.impl.DemandController;
import com.undercontroll.application.mapper.DemandDtoMapperImpl;
import com.undercontroll.domain.gateway.TokenGateway;
import com.undercontroll.domain.usecase.demand.CreateDemandPort;
import com.undercontroll.domain.usecase.demand.DeleteAllDemandsByOrderPort;
import com.undercontroll.domain.usecase.demand.DeleteDemandPort;
import com.undercontroll.domain.usecase.demand.DemandOutput;
import com.undercontroll.domain.usecase.demand.GetDemandsPort;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DemandController.class)
@Import({SecurityConfig.class, DemandDtoMapperImpl.class})
class DemandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateDemandPort createDemandPort;
    @MockitoBean
    private GetDemandsPort getDemandsPort;
    @MockitoBean
    private DeleteDemandPort deleteDemandPort;
    @MockitoBean
    private DeleteAllDemandsByOrderPort deleteAllDemandsByOrderPort;
    @MockitoBean
    private TokenGateway tokenGateway;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("creates a demand from the request body")
    void createDemand() throws Exception {
        when(createDemandPort.execute(5, new CreateDemandPort.Input(10, 3L))).thenReturn(new DemandOutput(25, 10, 5, 3L));

        mockMvc.perform(post("/v1/api/orders/5/demands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"componentPartId\":10,\"quantity\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(25))
                .andExpect(jsonPath("$.componentId").value(10))
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("lists and deletes demands of an order")
    void listAndDelete() throws Exception {
        when(getDemandsPort.execute(5, null)).thenReturn(List.of(new DemandOutput(25, 10, 5, 3L)));

        mockMvc.perform(get("/v1/api/orders/5/demands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(5));
        mockMvc.perform(delete("/v1/api/orders/5/demands/25")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/v1/api/orders/5/demands")).andExpect(status().isNoContent());

        verify(deleteDemandPort).execute(25);
        verify(deleteAllDemandsByOrderPort).execute(5);
    }
}
