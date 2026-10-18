package com.undercontroll.infrastructure.ai;

import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;
import com.google.genai.types.ThinkingLevel;
import com.undercontroll.domain.exception.TranscriptionUnavailableException;
import com.undercontroll.domain.gateway.AudioTranscriptionGateway;
import com.undercontroll.domain.model.AudioClip;
import com.undercontroll.domain.model.AudioFormat;
import com.undercontroll.domain.support.Durations;
import com.undercontroll.infrastructure.ai.config.AiFeatureSettings;
import com.undercontroll.infrastructure.ai.config.ModelCapabilities;
import com.undercontroll.infrastructure.ai.config.ThinkingPolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.Set;

@Slf4j
public class GeminiAudioTranscriptionAdapter implements AudioTranscriptionGateway {

    static final String USER_PROMPT =
            "Transcreva fielmente o áudio em português do Brasil. Responda só com o texto falado, sem aspas nem comentários.";

    static final Set<AudioFormat> SUPPORTED = Set.copyOf(EnumSet.allOf(AudioFormat.class));

    private final Models models;
    private final String model;
    private final ThinkingPolicy thinking;
    private final ModelCapabilities capabilities;

    public GeminiAudioTranscriptionAdapter(Client client, AiFeatureSettings settings) {
        this(client.models, settings);
    }

    GeminiAudioTranscriptionAdapter(Models models, AiFeatureSettings settings) {
        this.models = models;
        this.model = settings.model();
        this.thinking = settings.thinking();
        this.capabilities = settings.capabilities();
    }

    @Override
    public String transcribe(AudioClip audio) {
        long started = System.nanoTime();
        try {
            String mimeType = mimeType(audio.format());
            byte[] bytes = audio.content();
            log.info(
                    "Gemini transcription started model={} mime={} byteSize={}",
                    model,
                    mimeType,
                    bytes.length
            );
            Content content = capabilities.dedicatedTranscription()
                    ? Content.fromParts(Part.fromBytes(bytes, mimeType))
                    : Content.fromParts(
                            Part.fromText(USER_PROMPT),
                            Part.fromBytes(bytes, mimeType));
            GenerateContentResponse response = models.generateContent(model, content, config());
            String text = response == null ? null : response.text();
            if (!StringUtils.hasText(text)) {
                log.warn(
                        "Gemini transcription returned empty text model={} mime={} byteSize={} durationMs={}",
                        model,
                        mimeType,
                        bytes.length,
                        Durations.millisSince(started)
                );
                throw new TranscriptionUnavailableException();
            }
            log.info(
                    "Gemini transcription finished model={} mime={} byteSize={} chars={} durationMs={}",
                    model,
                    mimeType,
                    bytes.length,
                    text.trim().length(),
                    Durations.millisSince(started)
            );
            return text.trim();
        } catch (TranscriptionUnavailableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn(
                    "Gemini transcription failed model={} durationMs={} cause={}",
                    model,
                    Durations.millisSince(started),
                    ex.toString()
            );
            throw new TranscriptionUnavailableException(ex);
        }
    }

    GenerateContentConfig config() {
        GenerateContentConfig.Builder builder = GenerateContentConfig.builder();
        if (thinking == ThinkingPolicy.DEFAULT) {
            return builder.build();
        }
        ThinkingConfig.Builder thinkingConfig = ThinkingConfig.builder().includeThoughts(false);
        if (capabilities.thinkingControl() == ModelCapabilities.ThinkingControl.BUDGET) {
            thinkingConfig.thinkingBudget(thinking.budgetTokens());
        } else {
            thinkingConfig.thinkingLevel(new ThinkingLevel(thinking.name()));
        }
        return builder.thinkingConfig(thinkingConfig.build()).build();
    }

    @Override
    public Set<AudioFormat> supportedFormats() {
        return SUPPORTED;
    }

    static String mimeType(AudioFormat format) {
        return format == AudioFormat.MP3 ? "audio/mp3" : format.mimeType();
    }
}
