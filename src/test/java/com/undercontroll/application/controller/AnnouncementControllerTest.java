package com.undercontroll.application.controller;

import com.undercontroll.domain.model.PresignedUpload;
import com.undercontroll.domain.model.AccessTokenClaims;
import com.undercontroll.application.mapper.AnnouncementDtoMapperImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.application.dto.announcement.AnnouncementImageUploadDto;
import com.undercontroll.domain.exception.InvalidAnnouncementException;
import com.undercontroll.domain.enums.AnnouncementType;
import com.undercontroll.domain.gateway.TokenGateway;
import com.undercontroll.domain.usecase.announcement.*;
import com.undercontroll.infrastructure.config.SecurityConfig;
import com.undercontroll.infrastructure.config.RateLimitProperties;
import com.undercontroll.application.dto.announcement.CreateAnnouncementRequest;
import com.undercontroll.application.dto.announcement.UpdateAnnouncementRequest;
import com.undercontroll.application.controller.impl.AnnouncementController;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({SecurityConfig.class, RateLimitProperties.class, AnnouncementDtoMapperImpl.class})
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

    @MockitoBean
    private TokenGateway tokenGateway;

    private void mockTokenPortWithRole(String role) {
        when(tokenGateway.validateToken(anyString())).thenReturn(new AccessTokenClaims("user@example.com", role));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /v1/api/announcements
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /v1/api/announcements - ADMINISTRATOR should create announcement and return 201")
    void administratorShouldCreateAnnouncementSuccessfully() throws Exception {
        mockTokenPortWithRole("ADMINISTRATOR");

        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                "New Feature", "We have a new feature available!", null, AnnouncementType.HOLIDAY
        );

        CreateAnnouncementPort.Output response = new CreateAnnouncementPort.Output(
                1, "New Feature", "We have a new feature available!", null, AnnouncementType.HOLIDAY,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(createAnnouncement.execute(any(CreateAnnouncementPort.Input.class), anyString())).thenReturn(response);

        mockMvc.perform(post("/v1/api/announcements")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("New Feature"))
                .andExpect(jsonPath("$.type").value("HOLIDAY"));

        verify(createAnnouncement, times(1)).execute(any(CreateAnnouncementPort.Input.class), anyString());
    }

    @Test
    @DisplayName("POST /v1/api/announcements - ADMINISTRATOR should create announcement with upload url and return 201")
    void administratorShouldCreateAnnouncementWithUploadUrlSuccessfully() throws Exception {
        mockTokenPortWithRole("ADMINISTRATOR");

        AnnouncementImageUploadDto imageUpload = new AnnouncementImageUploadDto("cover.png", "image/png", 1024L);
        PresignedUpload uploadResponse = new PresignedUpload(
                "https://s3.example/upload",
                "announcements/1/cover.png",
                123L
        );
        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                "New Feature", "We have a new feature available!", imageUpload, AnnouncementType.HOLIDAY
        );

        CreateAnnouncementPort.Output response = new CreateAnnouncementPort.Output(
                1, "New Feature", "We have a new feature available!", uploadResponse, AnnouncementType.HOLIDAY,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(createAnnouncement.execute(any(CreateAnnouncementPort.Input.class), anyString())).thenReturn(response);

        mockMvc.perform(post("/v1/api/announcements")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.imageUpload.presigned_url").value("https://s3.example/upload"))
                .andExpect(jsonPath("$.imageUpload.file_key").value("announcements/1/cover.png"));

        verify(createAnnouncement, times(1)).execute(argThat(req ->
                req.imageUpload() != null
                        && req.imageUpload().originalName().equals("cover.png")
                        && req.imageUpload().contentType().equals("image/png")
        ), anyString());
    }

    @Test
    @DisplayName("POST /v1/api/announcements - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToCreateAnnouncement() throws Exception {
        mockTokenPortWithRole("SCOPE_CUSTOMER");

        CreateAnnouncementRequest request = new CreateAnnouncementRequest(
                "New Feature", "We have a new feature available!", null, AnnouncementType.HOLIDAY
        );

        mockMvc.perform(post("/v1/api/announcements")
                        .header("Authorization", "Bearer customer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(createAnnouncement, never()).execute(any(CreateAnnouncementPort.Input.class), anyString());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /v1/api/announcements
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /v1/api/announcements - should return 200 with paginated response")
    void administratorShouldGetAnnouncementsPaginatedSuccessfully() throws Exception {
        AnnouncementOutput announcement1 = new AnnouncementOutput(
                1, "Title 1", "Content 1", "https://s3.example/read-1", AnnouncementType.HOLIDAY, LocalDateTime.now(), LocalDateTime.now()
        );
        AnnouncementOutput announcement2 = new AnnouncementOutput(
                2, "Title 2", "Content 2", null, AnnouncementType.UPDATES, LocalDateTime.now(), LocalDateTime.now()
        );

        when(getAnnouncements.execute(any(), any(), any()))
                .thenReturn(new GetAnnouncementsPort.Output(List.of(announcement1, announcement2), 2L, 1, 0, 10));

        mockMvc.perform(get("/v1/api/announcements")
                        .with(user("admin@example.com").roles("ADMINISTRATOR"))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.announcements[0].title").value("Title 1"))
                .andExpect(jsonPath("$.announcements[0].imageUrl").value("https://s3.example/read-1"))
                .andExpect(jsonPath("$.announcements[1].title").value("Title 2"))
                .andExpect(jsonPath("$.announcements.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));

        verify(getAnnouncements, times(1)).execute(any(), any(), any());
    }

    @Test
    @DisplayName("GET /v1/api/announcements - should return 200 with empty list when no announcements found")
    void shouldReturn200WithEmptyListWhenNoAnnouncementsFound() throws Exception {
        when(getAnnouncements.execute(any(), any(), any()))
                .thenReturn(new GetAnnouncementsPort.Output(List.of(), 0L, 0, 0, 10));

        mockMvc.perform(get("/v1/api/announcements")
                        .with(user("admin@example.com").roles("ADMINISTRATOR"))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.announcements").isArray())
                .andExpect(jsonPath("$.announcements.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        verify(getAnnouncements, times(1)).execute(any(), any(), any());
    }

    @Test
    @DisplayName("GET /v1/api/announcements - should use default pagination values when params not provided")
    void shouldUseDefaultPaginationWhenParamsNotProvided() throws Exception {
        when(getAnnouncements.execute(any(), any(), any()))
                .thenReturn(new GetAnnouncementsPort.Output(List.of(), 0L, 0, 0, 10));

        mockMvc.perform(get("/v1/api/announcements")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));

        verify(getAnnouncements, times(1)).execute(any(), any(), any());
    }

    @Test
    @DisplayName("GET /v1/api/announcements - should filter by type when type param is provided")
    void shouldFilterByTypeWhenTypeParamIsProvided() throws Exception {
        AnnouncementOutput announcement = new AnnouncementOutput(
                1, "Promoção", "50% off", null, AnnouncementType.PROMOTIONS, LocalDateTime.now(), LocalDateTime.now()
        );

        when(getAnnouncements.execute(any(), any(), any()))
                .thenReturn(new GetAnnouncementsPort.Output(List.of(announcement), 1L, 1, 0, 10));

        mockMvc.perform(get("/v1/api/announcements")
                        .with(user("admin@example.com").roles("ADMINISTRATOR"))
                        .param("page", "0")
                        .param("size", "10")
                        .param("type", "PROMOTIONS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.announcements[0].type").value("PROMOTIONS"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(getAnnouncements, times(1)).execute(any(), any(), argThat(type ->
                type == AnnouncementType.PROMOTIONS
        ));
    }

    @Test
    @DisplayName("GET /v1/api/announcements - should pass null type when type param is not provided")
    void shouldPassNullTypeWhenTypeParamNotProvided() throws Exception {
        when(getAnnouncements.execute(any(), any(), any()))
                .thenReturn(new GetAnnouncementsPort.Output(List.of(), 0L, 0, 0, 10));

        mockMvc.perform(get("/v1/api/announcements")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isOk());

        verify(getAnnouncements, times(1)).execute(any(), any(), isNull());
    }

    @Test
    @DisplayName("GET /v1/api/announcements - CUSTOMER should be able to get announcements and return 200")
    void customerShouldGetAnnouncementsPaginatedSuccessfully() throws Exception {
        AnnouncementOutput announcement = new AnnouncementOutput(
                1, "Title 1", "Content 1", null, AnnouncementType.HOLIDAY, LocalDateTime.now(), LocalDateTime.now()
        );

        when(getAnnouncements.execute(any(), any(), any()))
                .thenReturn(new GetAnnouncementsPort.Output(List.of(announcement), 1L, 1, 0, 10));

        mockMvc.perform(get("/v1/api/announcements")
                        .with(user("customer@example.com").roles("SCOPE_CUSTOMER"))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.announcements[0].title").value("Title 1"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(getAnnouncements, times(1)).execute(any(), any(), any());
    }

    @Test
    @DisplayName("GET /v1/api/announcements - should return correct totalPages for multi-page result")
    void shouldReturnCorrectTotalPagesForMultiPageResult() throws Exception {
        List<AnnouncementOutput> pageContent = List.of(
                new AnnouncementOutput(1, "T1", "C1", null, AnnouncementType.HOLIDAY, LocalDateTime.now(), LocalDateTime.now()),
                new AnnouncementOutput(2, "T2", "C2", null, AnnouncementType.HOLIDAY, LocalDateTime.now(), LocalDateTime.now()),
                new AnnouncementOutput(3, "T3", "C3", null, AnnouncementType.HOLIDAY, LocalDateTime.now(), LocalDateTime.now())
        );

        when(getAnnouncements.execute(any(), any(), any()))
                .thenReturn(new GetAnnouncementsPort.Output(pageContent, 9L, 3, 0, 3));

        mockMvc.perform(get("/v1/api/announcements")
                        .with(user("admin@example.com").roles("ADMINISTRATOR"))
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.announcements.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(9))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.size").value(3));

        verify(getAnnouncements, times(1)).execute(any(), any(), any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /v1/api/announcements/{announcementId}
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /v1/api/announcements/{announcementId} - ADMINISTRATOR should update announcement and return 200")
    void administratorShouldUpdateAnnouncementSuccessfully() throws Exception {
        UpdateAnnouncementRequest request = new UpdateAnnouncementRequest(
                "Updated Title", "Updated Content", null, false, AnnouncementType.HOLIDAY
        );

        UpdateAnnouncementPort.Output response = new UpdateAnnouncementPort.Output(
                1, "Updated Title", "Updated Content", null, null, AnnouncementType.HOLIDAY,
                LocalDateTime.now(), LocalDateTime.now()
        );

        when(updateAnnouncement.execute(anyInt(), any(UpdateAnnouncementPort.Input.class))).thenReturn(response);

        mockMvc.perform(put("/v1/api/announcements/1")
                        .with(user("admin@example.com").roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.content").value("Updated Content"))
                .andExpect(jsonPath("$.type").value("HOLIDAY"));

        verify(updateAnnouncement, times(1)).execute(anyInt(), any(UpdateAnnouncementPort.Input.class));
    }

    @Test
    @DisplayName("PUT /v1/api/announcements/{announcementId} - ADMINISTRATOR should update image and return upload url")
    void administratorShouldUpdateAnnouncementImageSuccessfully() throws Exception {
        AnnouncementImageUploadDto imageUpload = new AnnouncementImageUploadDto("new-cover.webp", "image/webp", 2048L);
        UpdateAnnouncementRequest request = new UpdateAnnouncementRequest(
                "Updated Title", "Updated Content", imageUpload, false, AnnouncementType.HOLIDAY
        );
        PresignedUpload uploadResponse = new PresignedUpload(
                "https://s3.example/update",
                "announcements/1/new-cover.webp",
                456L
        );

        UpdateAnnouncementPort.Output response = new UpdateAnnouncementPort.Output(
                1,
                "Updated Title",
                "Updated Content",
                "https://s3.example/read",
                uploadResponse,
                AnnouncementType.HOLIDAY,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(updateAnnouncement.execute(anyInt(), any(UpdateAnnouncementPort.Input.class))).thenReturn(response);

        mockMvc.perform(put("/v1/api/announcements/1")
                        .with(user("admin@example.com").roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value("https://s3.example/read"))
                .andExpect(jsonPath("$.imageUpload.presigned_url").value("https://s3.example/update"))
                .andExpect(jsonPath("$.imageUpload.file_key").value("announcements/1/new-cover.webp"));

        verify(updateAnnouncement, times(1)).execute(anyInt(), argThat(req ->
                req.imageUpload() != null
                        && req.imageUpload().originalName().equals("new-cover.webp")
                        && !Boolean.TRUE.equals(req.removeImage())
        ));
    }

    @Test
    @DisplayName("PUT /v1/api/announcements/{announcementId} - ADMINISTRATOR should remove image")
    void administratorShouldRemoveAnnouncementImageSuccessfully() throws Exception {
        UpdateAnnouncementRequest request = new UpdateAnnouncementRequest(
                "Updated Title", "Updated Content", null, true, AnnouncementType.HOLIDAY
        );

        UpdateAnnouncementPort.Output response = new UpdateAnnouncementPort.Output(
                1,
                "Updated Title",
                "Updated Content",
                null,
                null,
                AnnouncementType.HOLIDAY,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(updateAnnouncement.execute(anyInt(), any(UpdateAnnouncementPort.Input.class))).thenReturn(response);

        mockMvc.perform(put("/v1/api/announcements/1")
                        .with(user("admin@example.com").roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").doesNotExist())
                .andExpect(jsonPath("$.imageUpload").doesNotExist());

        verify(updateAnnouncement, times(1)).execute(anyInt(), argThat(req ->
                Boolean.TRUE.equals(req.removeImage()) && req.imageUpload() == null
        ));
    }

    @Test
    @DisplayName("PUT /v1/api/announcements/{announcementId} - should return 400 when uploading and removing image")
    void shouldReturn400WhenUploadingAndRemovingAnnouncementImage() throws Exception {
        AnnouncementImageUploadDto imageUpload = new AnnouncementImageUploadDto("cover.png", "image/png", 1024L);
        UpdateAnnouncementRequest request = new UpdateAnnouncementRequest(
                "Updated Title", "Updated Content", imageUpload, true, AnnouncementType.HOLIDAY
        );

        when(updateAnnouncement.execute(anyInt(), any(UpdateAnnouncementPort.Input.class)))
                .thenThrow(new InvalidAnnouncementException("Cannot upload and remove an image at the same time"));

        mockMvc.perform(put("/v1/api/announcements/1")
                        .with(user("admin@example.com").roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(updateAnnouncement, times(1)).execute(anyInt(), any(UpdateAnnouncementPort.Input.class));
    }

    @Test
    @DisplayName("PUT /v1/api/announcements/{announcementId} - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToUpdateAnnouncement() throws Exception {
        UpdateAnnouncementRequest request = new UpdateAnnouncementRequest(
                "Updated Title", "Updated Content", null, false, AnnouncementType.HOLIDAY
        );

        mockMvc.perform(put("/v1/api/announcements/1")
                        .with(user("customer@example.com").roles("SCOPE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(updateAnnouncement, never()).execute(anyInt(), any(UpdateAnnouncementPort.Input.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /v1/api/announcements/{announcementId}
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /v1/api/announcements/{announcementId} - ADMINISTRATOR should delete announcement and return 204")
    void administratorShouldDeleteAnnouncementSuccessfully() throws Exception {
        doNothing().when(deleteAnnouncement).execute(anyInt());

        mockMvc.perform(delete("/v1/api/announcements/1")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isNoContent());

        verify(deleteAnnouncement, times(1)).execute(anyInt());
    }

    @Test
    @DisplayName("DELETE /v1/api/announcements/{announcementId} - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToDeleteAnnouncement() throws Exception {
        mockMvc.perform(delete("/v1/api/announcements/1")
                        .with(user("customer@example.com").roles("SCOPE_CUSTOMER")))
                .andExpect(status().isForbidden());

        verify(deleteAnnouncement, never()).execute(anyInt());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /v1/api/announcements/latest
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /v1/api/announcements/latest - should return 200 with latest announcement")
    void shouldReturnLastAnnouncementSuccessfully() throws Exception {
        AnnouncementOutput last = new AnnouncementOutput(
                5, "Last", "Last content", "https://s3.example/last", AnnouncementType.UPDATES, LocalDateTime.now(), LocalDateTime.now()
        );

        when(getLastAnnouncement.execute()).thenReturn(Optional.of(last));

        mockMvc.perform(get("/v1/api/announcements/latest")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.title").value("Last"))
                .andExpect(jsonPath("$.imageUrl").value("https://s3.example/last"))
                .andExpect(jsonPath("$.type").value("UPDATES"));

        verify(getLastAnnouncement, times(1)).execute();
    }

    @Test
    @DisplayName("GET /v1/api/announcements/latest - should return 404 when no announcement exists")
    void shouldReturn404WhenNoLastAnnouncementExists() throws Exception {
        when(getLastAnnouncement.execute()).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/api/announcements/latest")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isNotFound());

        verify(getLastAnnouncement, times(1)).execute();
    }
}
