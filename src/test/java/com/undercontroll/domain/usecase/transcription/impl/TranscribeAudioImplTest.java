package com.undercontroll.domain.usecase.transcription.impl;

import com.undercontroll.domain.exception.InvalidTranscriptionException;
import com.undercontroll.domain.exception.TranscriptionUnavailableException;
import com.undercontroll.domain.gateway.AudioTranscriptionGateway;
import com.undercontroll.domain.model.AudioClip;
import com.undercontroll.domain.model.AudioFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscribeAudioImplTest {

    @Mock
    private ObjectProvider<AudioTranscriptionGateway> audioTranscriptionGateway;

    @Mock
    private AudioTranscriptionGateway gateway;

    private TranscribeAudioImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new TranscribeAudioImpl(audioTranscriptionGateway, Duration.ofSeconds(180));
    }

    @Test
    @DisplayName("sends the clip with the detected format, canonical MIME and extension")
    void transcribesNormalizedClip() throws IOException {
        ready(EnumSet.allOf(AudioFormat.class));
        when(gateway.transcribe(any())).thenReturn("  Geladeira não gela  ");

        String text = useCase.execute(new AudioClip(fixture("note.wav"), "audio/x-wav", "gravacao.WAV"));

        ArgumentCaptor<AudioClip> sent = ArgumentCaptor.forClass(AudioClip.class);
        verify(gateway).transcribe(sent.capture());
        assertThat(text).isEqualTo("Geladeira não gela");
        assertThat(sent.getValue().format()).isEqualTo(AudioFormat.WAV);
        assertThat(sent.getValue().contentType()).isEqualTo("audio/wav");
        assertThat(sent.getValue().filename()).isEqualTo("gravacao.wav");
    }

    @Test
    @DisplayName("content decides the format when the MIME is missing or generic, whatever the extension says")
    void contentWins() throws IOException {
        AudioClip octet = useCase.validate(new AudioClip(fixture("note.m4a"), "application/octet-stream", "audio.webm"));
        AudioClip none = useCase.validate(new AudioClip(fixture("note.webm"), null, null));

        assertThat(octet.format()).isEqualTo(AudioFormat.M4A);
        assertThat(octet.filename()).isEqualTo("audio.m4a");
        assertThat(none.format()).isEqualTo(AudioFormat.WEBM);
    }

    @Test
    @DisplayName("a declared MIME that contradicts the content is rejected with both formats named")
    void mimeConflict() throws IOException {
        AudioClip clip = new AudioClip(fixture("note.webm"), "audio/wav", "note.wav");

        assertThatThrownBy(() -> useCase.execute(clip))
                .isInstanceOf(InvalidTranscriptionException.class)
                .hasMessageContaining("audio/wav")
                .hasMessageContaining("webm");
    }

    @Test
    @DisplayName("non-audio MIME and unrecognized content are rejected")
    void rejectsNonAudio() throws IOException {
        AudioClip textMime = new AudioClip(fixture("note.wav"), "text/plain", "note.wav");
        AudioClip garbage = new AudioClip("not audio".getBytes(), "audio/webm", "note.webm");

        assertThatThrownBy(() -> useCase.execute(textMime)).hasMessageContaining("Unsupported audio type text/plain");
        assertThatThrownBy(() -> useCase.execute(garbage)).hasMessageContaining("unrecognized audio content");
    }

    @Test
    @DisplayName("WAV longer than the maximum duration is rejected")
    void rejectsLongWav() throws IOException {
        TranscribeAudioImpl strict = new TranscribeAudioImpl(audioTranscriptionGateway, Duration.ofMillis(100));
        AudioClip clip = new AudioClip(fixture("note.wav"), "audio/wav", "note.wav");

        assertThatThrownBy(() -> strict.execute(clip)).hasMessageContaining("longer than");
    }

    @Test
    @DisplayName("a format the provider does not accept is a 400, not a provider error")
    void providerCapabilities() throws IOException {
        ready(EnumSet.of(AudioFormat.WAV));
        AudioClip clip = new AudioClip(fixture("note.ogg"), "audio/ogg", "note.ogg");

        assertThatThrownBy(() -> useCase.execute(clip))
                .isInstanceOf(InvalidTranscriptionException.class)
                .hasMessageContaining("ogg");
    }

    @Test
    @DisplayName("returns 503 when no transcription gateway is configured")
    void unavailableWithoutGateway() throws IOException {
        when(audioTranscriptionGateway.getIfAvailable()).thenReturn(null);
        AudioClip clip = new AudioClip(fixture("note.mp3"), "audio/mpeg", "note.mp3");

        assertThatThrownBy(() -> useCase.execute(clip)).isInstanceOf(TranscriptionUnavailableException.class);
    }

    @Test
    @DisplayName("blank text and provider failures are unavailable")
    void providerFailures() throws IOException {
        ready(EnumSet.allOf(AudioFormat.class));
        when(gateway.transcribe(any())).thenReturn(" ").thenThrow(new IllegalStateException("down"));
        AudioClip clip = new AudioClip(fixture("note.mp3"), "audio/mpeg", "note.mp3");

        assertThatThrownBy(() -> useCase.execute(clip)).isInstanceOf(TranscriptionUnavailableException.class);
        assertThatThrownBy(() -> useCase.execute(clip)).isInstanceOf(TranscriptionUnavailableException.class);
    }

    @Test
    @DisplayName("requires a non-empty clip within 8MB")
    void sizeRules() {
        assertThatThrownBy(() -> useCase.execute(null)).isInstanceOf(InvalidTranscriptionException.class);
        assertThatThrownBy(() -> useCase.execute(new AudioClip(new byte[0], "audio/webm", "note.webm")))
                .hasMessageContaining("empty");
        assertThatThrownBy(() -> useCase.execute(new AudioClip(new byte[(int) TranscribeAudioImpl.MAX_BYTES + 1], "audio/webm", "n.webm")))
                .hasMessageContaining("8MB");
    }

    private void ready(Set<AudioFormat> formats) {
        when(audioTranscriptionGateway.getIfAvailable()).thenReturn(gateway);
        when(gateway.supportedFormats()).thenReturn(formats);
    }

    private static byte[] fixture(String name) throws IOException {
        try (InputStream in = TranscribeAudioImplTest.class.getResourceAsStream("/audio/" + name)) {
            return in.readAllBytes();
        }
    }
}
