package com.undercontroll.application.mapper;

import com.undercontroll.application.dto.transcription.TranscribeAudioResponse;
import com.undercontroll.domain.exception.TranscriptionUnavailableException;
import com.undercontroll.domain.model.AudioClip;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TranscriptionDtoMapperTest {

    private final TranscriptionDtoMapper mapper = new TranscriptionDtoMapper();

    @Test
    @DisplayName("maps a multipart file to an audio clip")
    void toClip() {
        MockMultipartFile audio = new MockMultipartFile(
                "audio", "note.webm", "audio/webm", "voice".getBytes()
        );

        AudioClip clip = mapper.toClip(audio);

        assertArrayEquals("voice".getBytes(), clip.content());
        assertEquals("audio/webm", clip.contentType());
        assertEquals("note.webm", clip.filename());
        assertEquals(5, clip.size());
    }

    @Test
    @DisplayName("maps null audio to a null clip")
    void toClipNull() {
        assertNull(mapper.toClip(null));
    }

    @Test
    @DisplayName("reports unreadable uploads as transcription unavailable")
    void unreadableUpload() throws IOException {
        MultipartFile audio = mock(MultipartFile.class);
        when(audio.getBytes()).thenThrow(new IOException("disk"));

        assertThrows(TranscriptionUnavailableException.class, () -> mapper.toClip(audio));
    }

    @Test
    @DisplayName("maps transcribed text to a response")
    void toResponse() {
        TranscribeAudioResponse response = mapper.toResponse("Geladeira não gela");

        assertEquals("Geladeira não gela", response.text());
    }
}
