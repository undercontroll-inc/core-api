package com.undercontroll.application.controller;

import com.undercontroll.application.controller.impl.MarketInsightsController;
import com.undercontroll.domain.usecase.insights.GetMarketInsightsPort;
import com.undercontroll.infrastructure.config.SecurityConfig;
import com.undercontroll.domain.gateway.TokenGateway;
import com.undercontroll.domain.exception.MarketDataUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class})
@AutoConfigureMockMvc
@WebMvcTest(MarketInsightsController.class)
class MarketInsightsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetMarketInsightsPort getMarketInsightsPort;

    @MockitoBean
    private TokenGateway tokenGateway;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("GET /v1/api/insights returns 200 with empty lists when the monthly batch is missing")
    void emptyInsights() throws Exception {
        when(getMarketInsightsPort.execute()).thenReturn(GetMarketInsightsPort.Output.empty());

        mockMvc.perform(get("/v1/api/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insights").isEmpty())
                .andExpect(jsonPath("$.risingProducts").isEmpty())
                .andExpect(jsonPath("$.limitations").isEmpty());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("GET /v1/api/insights is forbidden for customers")
    void getForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/v1/api/insights"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("GET /v1/api/insights returns 503 when market views fail")
    void marketDataUnavailable() throws Exception {
        when(getMarketInsightsPort.execute()).thenThrow(new MarketDataUnavailableException(new IllegalStateException("db down")));

        mockMvc.perform(get("/v1/api/insights"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(MarketDataUnavailableException.CODE));
    }
}
