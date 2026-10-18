package com.undercontroll.infrastructure.ai;

import com.undercontroll.infrastructure.metrics.TestMetrics;
import com.google.genai.Models;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.undercontroll.domain.exception.TranscriptionUnavailableException;
import com.undercontroll.domain.model.AudioClip;
import com.undercontroll.domain.model.AudioFormat;
import com.undercontroll.infrastructure.ai.config.AiFeatureSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiAudioTranscriptionAdapterTest {

    @Mock
    private Models models;

    @Mock
    private GenerateContentResponse response;

    @Test
    @DisplayName("returns Gemini text")
    void transcribes() {
        when(models.generateContent(eq("gemini-3.5-flash-lite"), any(Content.class), any(GenerateContentConfig.class)))
                .thenReturn(response);
        when(response.text()).thenReturn("  Compressor ruindo ");

        String text = new GeminiAudioTranscriptionAdapter(models, settings("gemini-3.5-flash-lite", "minimal"), TestMetrics.feature("transcription"))
                .transcribe(new AudioClip("bytes".getBytes(), "audio/webm", "note.webm", AudioFormat.WEBM));

        assertEquals("Compressor ruindo", text);
    }

    @Test
    @DisplayName("treats blank Gemini output as unavailable")
    void blankOutput() {
        when(models.generateContent(eq("gemini-3.5-flash-lite"), any(Content.class), any(GenerateContentConfig.class)))
                .thenReturn(response);
        when(response.text()).thenReturn(" ");
        GeminiAudioTranscriptionAdapter adapter = new GeminiAudioTranscriptionAdapter(models, settings("gemini-3.5-flash-lite", "minimal"), TestMetrics.feature("transcription"));
        AudioClip audio = new AudioClip("bytes".getBytes(), "audio/webm", "note.webm", AudioFormat.WEBM);

        assertThrows(TranscriptionUnavailableException.class, () ->
                adapter.transcribe(audio));
    }

    @Test
    @DisplayName("sends provider MIME types and declares the formats it accepts")
    void providerMime() {
        assertEquals("audio/mp3", GeminiAudioTranscriptionAdapter.mimeType(AudioFormat.MP3));
        assertEquals("audio/mp4", GeminiAudioTranscriptionAdapter.mimeType(AudioFormat.M4A));
        assertEquals("audio/wav", GeminiAudioTranscriptionAdapter.mimeType(AudioFormat.WAV));
        assertTrue(new GeminiAudioTranscriptionAdapter(models, settings("gemini-3.5-flash-lite", "minimal"), TestMetrics.feature("transcription"))
                .supportedFormats().containsAll(EnumSet.allOf(AudioFormat.class)));
    }

    @Test
    @DisplayName("sends the configured thinking level for Gemini 3 transcription")
    void minimalThinkingOnGemini3() {
        GenerateContentConfig config = new GeminiAudioTranscriptionAdapter(models, settings("gemini-3.5-flash-lite", "minimal"), TestMetrics.feature("transcription"))
                .config();

        assertEquals("MINIMAL", config.thinkingConfig().orElseThrow().thinkingLevel().orElseThrow().toString());
    }

    @Test
    @DisplayName("uses a thinking budget on Gemini 2.5 and nothing on dedicated transcribe models")
    void thinkingByCapability() {
        GenerateContentConfig budget = new GeminiAudioTranscriptionAdapter(models, settings("gemini-2.5-flash", "off"), TestMetrics.feature("transcription"))
                .config();
        GenerateContentConfig dedicated = new GeminiAudioTranscriptionAdapter(models, settings("gemini-3-transcribe", "minimal"), TestMetrics.feature("transcription"))
                .config();

        assertEquals(0, budget.thinkingConfig().orElseThrow().thinkingBudget().orElseThrow());
        assertTrue(dedicated.thinkingConfig().isEmpty());
    }

    private static AiFeatureSettings settings(String model, String thinking) {
        return AiFeatureSettings.transcription("transcription", "gemini", model, Duration.ofSeconds(60), thinking);
    }
}
