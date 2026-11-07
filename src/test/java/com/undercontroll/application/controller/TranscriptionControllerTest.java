package com.undercontroll.application.controller;

import com.undercontroll.application.controller.impl.TranscriptionController;
import com.undercontroll.application.mapper.TranscriptionDtoMapper;
import com.undercontroll.domain.exception.InvalidTranscriptionException;
import com.undercontroll.domain.exception.TranscriptionUnavailableException;
import com.undercontroll.domain.usecase.transcription.TranscribeAudioPort;
import com.undercontroll.infrastructure.config.SecurityConfig;
import com.undercontroll.infrastructure.handler.TranscriptionExceptionHandler;
import com.undercontroll.domain.gateway.TokenGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({
        SecurityConfig.class,
        TranscriptionExceptionHandler.class,
        TranscriptionDtoMapper.class
})
@AutoConfigureMockMvc
@WebMvcTest(TranscriptionController.class)
class TranscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TranscribeAudioPort transcribeAudioPort;

    @MockitoBean
    private TokenGateway tokenGateway;

    private static MockMultipartFile audio() {
        return new MockMultipartFile("audio", "note.webm", "audio/webm", "voice".getBytes());
    }

    @Test
    @WithMockUser(username = "12", roles = "ADMINISTRATOR")
    @DisplayName("POST /v1/api/transcriptions returns transcribed text")
    void administratorTranscribes() throws Exception {
        when(transcribeAudioPort.execute(any())).thenReturn("Geladeira não gela");

        mockMvc.perform(multipart("/v1/api/transcriptions").file(audio()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Geladeira não gela"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("POST /v1/api/transcriptions is forbidden for customers")
    void customerForbidden() throws Exception {
        mockMvc.perform(multipart("/v1/api/transcriptions").file(audio()))
                .andExpect(status().isForbidden());
        verify(transcribeAudioPort, never()).execute(any());
    }

    @Test
    @DisplayName("POST /v1/api/transcriptions is unauthorized without a token")
    void unauthenticated() throws Exception {
        mockMvc.perform(multipart("/v1/api/transcriptions").file(audio()))
                .andExpect(status().isUnauthorized());
        verify(transcribeAudioPort, never()).execute(any());
    }

    @Test
    @WithMockUser(username = "12", roles = "ADMINISTRATOR")
    @DisplayName("POST /v1/api/transcriptions returns 503 when speech-to-text is down")
    void unavailable() throws Exception {
        when(transcribeAudioPort.execute(any())).thenThrow(new TranscriptionUnavailableException());

        mockMvc.perform(multipart("/v1/api/transcriptions").file(audio()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("TRANSCRIPTION_UNAVAILABLE"));
    }

    @Test
    @WithMockUser(username = "12", roles = "ADMINISTRATOR")
    @DisplayName("POST /v1/api/transcriptions returns 400 for invalid audio")
    void invalidAudio() throws Exception {
        when(transcribeAudioPort.execute(any()))
                .thenThrow(new InvalidTranscriptionException("Audio file is empty"));

        mockMvc.perform(multipart("/v1/api/transcriptions").file(audio()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRANSCRIPTION_INVALID"));
    }

    @Test
    @WithMockUser(username = "12", roles = "ADMINISTRATOR")
    @DisplayName("POST /v1/api/transcriptions returns 400 when the audio part is missing")
    void missingAudioPart() throws Exception {
        mockMvc.perform(multipart("/v1/api/transcriptions").contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRANSCRIPTION_INVALID"));
        verify(transcribeAudioPort, never()).execute(any());
    }
}
