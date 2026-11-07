package com.undercontroll.application.controller;

import com.undercontroll.application.controller.impl.MarketInsightJobsController;
import com.undercontroll.domain.enums.InsightJobStatus;
import com.undercontroll.domain.exception.InsightsDisabledException;
import com.undercontroll.domain.exception.MarketSnapshotAwaitingException;
import com.undercontroll.domain.gateway.TokenGateway;
import com.undercontroll.domain.model.insight.InsightJob;
import com.undercontroll.domain.usecase.insights.RequestInsightJobPort;
import com.undercontroll.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class})
@AutoConfigureMockMvc
@WebMvcTest(MarketInsightJobsController.class)
class MarketInsightJobsControllerTest {

    private static final String PATH = "/v1/api/market/insights/jobs";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestInsightJobPort requestInsightJobPort;

    @MockitoBean
    private TokenGateway tokenGateway;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("POST returns 202 with the job in every profile")
    void accepted() throws Exception {
        Instant next = Instant.parse("2026-09-02T04:00:00Z");
        when(requestInsightJobPort.execute()).thenReturn(new InsightJob(7L, 10L, "2026-08", "7", "h", "gemini", "m",
                InsightJobStatus.PENDING, 0, null, null, next, null, next, next, null));

        mockMvc.perform(post(PATH))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(7))
                .andExpect(jsonPath("$.snapshotId").value(10))
                .andExpect(jsonPath("$.bucketKey").value("2026-08"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.nextRunAt").value("2026-09-02T04:00:00Z"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("POST returns 409 AWAITING_DATA before the first publication and 503 when disabled")
    void errors() throws Exception {
        when(requestInsightJobPort.execute()).thenThrow(new MarketSnapshotAwaitingException());
        mockMvc.perform(post(PATH))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AWAITING_DATA"));

        doThrow(new InsightsDisabledException()).when(requestInsightJobPort).execute();
        mockMvc.perform(post(PATH))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("INSIGHTS_DISABLED"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("POST is forbidden for customers")
    void forbidden() throws Exception {
        mockMvc.perform(post(PATH)).andExpect(status().isForbidden());

        verifyNoInteractions(requestInsightJobPort);
    }

    @Test
    @DisplayName("POST requires a token")
    void unauthorized() throws Exception {
        mockMvc.perform(post(PATH)).andExpect(status().isUnauthorized());
    }
}
