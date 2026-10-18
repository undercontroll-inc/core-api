package com.undercontroll.domain.usecase.transcription.impl;

import com.undercontroll.domain.exception.InvalidTranscriptionException;
import com.undercontroll.domain.exception.TranscriptionUnavailableException;
import com.undercontroll.domain.gateway.AudioTranscriptionGateway;
import com.undercontroll.domain.model.AudioClip;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscribeAudioImplTest {

    @Mock
    private ObjectProvider<AudioTranscriptionGateway> audioTranscriptionGateway;

    @Mock
    private AudioTranscriptionGateway gateway;

    @InjectMocks
    private TranscribeAudioImpl useCase;

    @Test
    @DisplayName("returns trimmed text from the gateway")
    void transcribes() {
        AudioClip audio = clip("audio/webm", "note.webm");
        when(audioTranscriptionGateway.getIfAvailable()).thenReturn(gateway);
        when(gateway.transcribe(audio)).thenReturn("  Geladeira não gela  ");

        assertEquals("Geladeira não gela", useCase.execute(audio));
    }

    @Test
    @DisplayName("returns 503 when no transcription gateway is configured")
    void unavailableWithoutGateway() {
        when(audioTranscriptionGateway.getIfAvailable()).thenReturn(null);
        AudioClip audio = clip("audio/webm", "note.webm");

        assertThrows(TranscriptionUnavailableException.class, () -> useCase.execute(audio));
    }

    @Test
    @DisplayName("treats a blank transcription as unavailable")
    void blankTranscription() {
        AudioClip audio = clip("audio/webm", "note.webm");
        when(audioTranscriptionGateway.getIfAvailable()).thenReturn(gateway);
        when(gateway.transcribe(audio)).thenReturn("  ");

        assertThrows(TranscriptionUnavailableException.class, () -> useCase.execute(audio));
    }

    @Test
    @DisplayName("requires an audio clip")
    void requiresAudio() {
        assertThrows(InvalidTranscriptionException.class, () -> useCase.execute(null));
    }

    @Test
    @DisplayName("rejects an empty file")
    void rejectsEmptyFile() {
        AudioClip audio = new AudioClip(new byte[0], "audio/webm", "note.webm");

        assertThrows(InvalidTranscriptionException.class, () -> useCase.execute(audio));
    }

    @Test
    @DisplayName("rejects files above 8MB")
    void rejectsOversizedFile() {
        AudioClip audio = new AudioClip(new byte[(int) TranscribeAudioImpl.MAX_BYTES + 1], "audio/webm", "note.webm");

        assertThrows(InvalidTranscriptionException.class, () -> useCase.execute(audio));
    }

    @Test
    @DisplayName("rejects an unsupported type")
    void rejectsUnsupportedType() {
        AudioClip audio = clip("text/plain", "note.txt");

        assertThrows(InvalidTranscriptionException.class, () -> useCase.execute(audio));
    }

    @Test
    @DisplayName("accepts octet-stream when the filename extension is audio")
    void acceptsOctetStreamWithAudioExtension() {
        AudioClip audio = clip("application/octet-stream", "note.m4a");
        when(audioTranscriptionGateway.getIfAvailable()).thenReturn(gateway);
        when(gateway.transcribe(any())).thenReturn("ok");

        assertEquals("ok", useCase.execute(audio));
    }

    @Test
    @DisplayName("wraps provider failures as unavailable")
    void wrapsProviderFailure() {
        when(audioTranscriptionGateway.getIfAvailable()).thenReturn(gateway);
        when(gateway.transcribe(any())).thenThrow(new IllegalStateException("timeout"));
        AudioClip audio = clip("audio/webm", "note.webm");

        assertThrows(TranscriptionUnavailableException.class, () -> useCase.execute(audio));
    }

    private static AudioClip clip(String contentType, String filename) {
        return new AudioClip("audio-bytes".getBytes(), contentType, filename);
    }
}
