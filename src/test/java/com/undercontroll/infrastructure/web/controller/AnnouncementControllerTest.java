package com.undercontroll.infrastructure.web.controller;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.application.dto.AnnouncementDto;
import com.undercontroll.domain.model.enums.AnnouncementType;
import com.undercontroll.domain.port.in.*;
import com.undercontroll.domain.port.out.TokenPort;
import com.undercontroll.infrastructure.config.SecurityConfig;
import com.undercontroll.infrastructure.web.dto.CreateAnnouncementRequest;
import com.undercontroll.infrastructure.web.dto.CreateAnnouncementResponse;
import com.undercontroll.infrastructure.web.dto.UpdateAnnouncementRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = true)
@WebMvcTest(AnnouncementController.class)
class AnnouncementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateAnnouncementPort createAnnouncement;

    @MockitoBean
    private GetAnnouncementsPort getAnnouncements;

    @MockitoBean
    private UpdateAnnouncementPort updateAnnouncement;

    @MockitoBean
    private DeleteAnnouncementPort deleteAnnouncement;

    @MockitoBean
    private GetLastAnnouncementPort getLastAnnouncement;

    // Required because AuthContextFilter depends on TokenPort
    @MockitoBean
    private TokenPort tokenPort;

    private void mockTokenPortWithRole(String role) {
        Claim claim = mock(Claim.class);
        when(claim.asString()).thenReturn(role);
        DecodedJWT decoded = mock(DecodedJWT.class);
        when(decoded.getSubject()).thenReturn("user@example.com");
        when(decoded.getClaim("roles")).thenReturn(claim);
        when(tokenPort.validateToken(anyString())).thenReturn(decoded);
    }

    // createAnnouncement requires @RequestHeader("Authorization") so the filter runs.
    // For all other tests, user() post-processor sets auth without an Authorization header,
    // so the filter passes through without calling tokenPort.

    @Test
    @DisplayName("POST /v1/api/announcements - ADMINISTRATOR should create announcement and return 201")
    void administratorShouldCreateAnnouncementSuccessfully() throws Exception {
        mockTokenPortWithRole("ADMINISTRATOR");

        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                "New Feature", "We have a new feature available!", AnnouncementType.HOLIDAY
        );

        CreateAnnouncementPort.Output output = new CreateAnnouncementPort.Output(
                1, "New Feature", "We have a new feature available!", AnnouncementType.HOLIDAY,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(createAnnouncement.execute(any(CreateAnnouncementPort.Input.class))).thenReturn(output);

        mockMvc.perform(post("/v1/api/announcements")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("New Feature"))
                .andExpect(jsonPath("$.type").value("HOLIDAY"));

        verify(createAnnouncement, times(1)).execute(any(CreateAnnouncementPort.Input.class));
    }

    @Test
    @DisplayName("POST /v1/api/announcements - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToCreateAnnouncement() throws Exception {
        mockTokenPortWithRole("SCOPE_CUSTOMER");

        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                "New Feature", "We have a new feature available!", AnnouncementType.HOLIDAY
        );

        mockMvc.perform(post("/v1/api/announcements")
                        .header("Authorization", "Bearer customer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(createAnnouncement, never()).execute(any(CreateAnnouncementPort.Input.class));
    }

    @Test
    @DisplayName("GET /v1/api/announcements - ADMINISTRATOR should get announcements paginated and return 200")
    void administratorShouldGetAnnouncementsPaginatedSuccessfully() throws Exception {
        AnnouncementDto announcement1 = new AnnouncementDto(
                1, "Title 1", "Content 1", AnnouncementType.HOLIDAY, LocalDateTime.now(), LocalDateTime.now()
        );
        AnnouncementDto announcement2 = new AnnouncementDto(
                2, "Title 2", "Content 2", AnnouncementType.HOLIDAY, LocalDateTime.now(), LocalDateTime.now()
        );

        when(getAnnouncements.execute(any(GetAnnouncementsPort.Input.class)))
                .thenReturn(new GetAnnouncementsPort.Output(List.of(announcement1, announcement2)));

        mockMvc.perform(get("/v1/api/announcements")
                        .with(user("admin@example.com").roles("ADMINISTRATOR"))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Title 1"))
                .andExpect(jsonPath("$[1].title").value("Title 2"))
                .andExpect(jsonPath("$.length()").value(2));

        verify(getAnnouncements, times(1)).execute(any(GetAnnouncementsPort.Input.class));
    }

    @Test
    @DisplayName("GET /v1/api/announcements - Should return 204 when no announcements found")
    void shouldReturn204WhenNoAnnouncementsFound() throws Exception {
        when(getAnnouncements.execute(any(GetAnnouncementsPort.Input.class)))
                .thenReturn(new GetAnnouncementsPort.Output(List.of()));

        mockMvc.perform(get("/v1/api/announcements")
                        .with(user("admin@example.com").roles("ADMINISTRATOR"))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isNoContent());

        verify(getAnnouncements, times(1)).execute(any(GetAnnouncementsPort.Input.class));
    }

    @Test
    @DisplayName("GET /v1/api/announcements - Should use default pagination when params not provided")
    void shouldUseDefaultPaginationWhenParamsNotProvided() throws Exception {
        when(getAnnouncements.execute(any(GetAnnouncementsPort.Input.class)))
                .thenReturn(new GetAnnouncementsPort.Output(List.of()));

        mockMvc.perform(get("/v1/api/announcements")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isNoContent());

        verify(getAnnouncements, times(1)).execute(any(GetAnnouncementsPort.Input.class));
    }

    @Test
    @DisplayName("GET /v1/api/announcements - CUSTOMER should be able to get announcements and return 200")
    void customerShouldGetAnnouncementsPaginatedSuccessfully() throws Exception {
        AnnouncementDto announcement1 = new AnnouncementDto(
                1, "Title 1", "Content 1", AnnouncementType.HOLIDAY, LocalDateTime.now(), LocalDateTime.now()
        );

        when(getAnnouncements.execute(any(GetAnnouncementsPort.Input.class)))
                .thenReturn(new GetAnnouncementsPort.Output(List.of(announcement1)));

        mockMvc.perform(get("/v1/api/announcements")
                        .with(user("customer@example.com").roles("SCOPE_CUSTOMER"))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Title 1"));

        verify(getAnnouncements, times(1)).execute(any(GetAnnouncementsPort.Input.class));
    }

    @Test
    @DisplayName("PUT /v1/api/announcements/{announcementId} - ADMINISTRATOR should update announcement and return 200")
    void administratorShouldUpdateAnnouncementSuccessfully() throws Exception {
        UpdateAnnouncementRequest request = new UpdateAnnouncementRequest(
                "Updated Title", "Updated Content", AnnouncementType.HOLIDAY
        );

        UpdateAnnouncementPort.Output output = new UpdateAnnouncementPort.Output(
                1, "Updated Title", "Updated Content", AnnouncementType.HOLIDAY,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(updateAnnouncement.execute(any(UpdateAnnouncementPort.Input.class))).thenReturn(output);

        mockMvc.perform(put("/v1/api/announcements/1")
                        .with(user("admin@example.com").roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.content").value("Updated Content"))
                .andExpect(jsonPath("$.type").value("HOLIDAY"));

        verify(updateAnnouncement, times(1)).execute(any(UpdateAnnouncementPort.Input.class));
    }

    @Test
    @DisplayName("PUT /v1/api/announcements/{announcementId} - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToUpdateAnnouncement() throws Exception {
        UpdateAnnouncementRequest request = new UpdateAnnouncementRequest(
                "Updated Title", "Updated Content", AnnouncementType.HOLIDAY
        );

        mockMvc.perform(put("/v1/api/announcements/1")
                        .with(user("customer@example.com").roles("SCOPE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(updateAnnouncement, never()).execute(any(UpdateAnnouncementPort.Input.class));
    }

    @Test
    @DisplayName("DELETE /v1/api/announcements/{announcementId} - ADMINISTRATOR should delete announcement and return 200")
    void administratorShouldDeleteAnnouncementSuccessfully() throws Exception {
        doNothing().when(deleteAnnouncement).execute(any(DeleteAnnouncementPort.Input.class));

        mockMvc.perform(delete("/v1/api/announcements/1")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isOk());

        verify(deleteAnnouncement, times(1)).execute(any(DeleteAnnouncementPort.Input.class));
    }

    @Test
    @DisplayName("DELETE /v1/api/announcements/{announcementId} - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToDeleteAnnouncement() throws Exception {
        mockMvc.perform(delete("/v1/api/announcements/1")
                        .with(user("customer@example.com").roles("SCOPE_CUSTOMER")))
                .andExpect(status().isForbidden());

        verify(deleteAnnouncement, never()).execute(any(DeleteAnnouncementPort.Input.class));
    }
}
