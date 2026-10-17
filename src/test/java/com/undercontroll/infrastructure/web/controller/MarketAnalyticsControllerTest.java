package com.undercontroll.infrastructure.web.controller;

import com.undercontroll.application.controller.impl.MarketAnalyticsController;
import com.undercontroll.application.dto.analytics.MarketAnalyticsResponse;
import com.undercontroll.domain.usecase.analytics.GetMarketAnalyticsPort;
import com.undercontroll.infrastructure.config.RateLimitProperties;
import com.undercontroll.infrastructure.config.SecurityConfig;
import com.undercontroll.domain.gateway.TokenGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class, RateLimitProperties.class})
@AutoConfigureMockMvc
@WebMvcTest(MarketAnalyticsController.class)
class MarketAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetMarketAnalyticsPort getMarketAnalyticsPort;

    @MockitoBean
    private TokenGateway tokenGateway;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("GET /v1/api/analytics returns 200 with empty lists when there is no bucket")
    void emptyAnalytics() throws Exception {
        when(getMarketAnalyticsPort.execute()).thenReturn(MarketAnalyticsResponse.empty());

        mockMvc.perform(get("/v1/api/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(0))
                .andExpect(jsonPath("$.topBrands").isEmpty())
                .andExpect(jsonPath("$.topCategories").isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("GET /v1/api/analytics returns ranking highlights for the current bucket")
    void analyticsOk() throws Exception {
        when(getMarketAnalyticsPort.execute()).thenReturn(
                new MarketAnalyticsResponse(
                        "2026-08",
                        20L,
                        8L,
                        List.of(new MarketAnalyticsResponse.BrandHighlight("Mondial", "mondial", 2L, 1, 81.3)),
                        List.of(new MarketAnalyticsResponse.CategoryHighlight("MLB-MICROWAVES", "Micro-ondas", 5L, 77.1, 2L))
                )
        );

        mockMvc.perform(get("/v1/api/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bucketKey").value("2026-08"))
                .andExpect(jsonPath("$.totalProducts").value(20))
                .andExpect(jsonPath("$.topBrands[0].name").value("Mondial"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("GET /v1/api/analytics is forbidden for customers")
    void forbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/v1/api/analytics"))
                .andExpect(status().isForbidden());
    }
}
