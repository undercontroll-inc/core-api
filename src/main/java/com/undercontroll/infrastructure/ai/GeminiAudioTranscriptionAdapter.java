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
import com.undercontroll.domain.support.Durations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Slf4j
public class GeminiAudioTranscriptionAdapter implements AudioTranscriptionGateway {

    static final String USER_PROMPT =
            "Transcreva fielmente o áudio em português do Brasil. Responda só com o texto falado, sem aspas nem comentários.";

    private final Models models;
    private final String model;

    public GeminiAudioTranscriptionAdapter(Client client, String model) {
        this(client.models, model);
    }

    GeminiAudioTranscriptionAdapter(Models models, String model) {
        this.models = models;
        this.model = model;
    }

    @Override
    public String transcribe(AudioClip audio) {
        long started = System.nanoTime();
        try {
            MimeType mimeType = mimeType(audio.contentType(), audio.filename());
            byte[] bytes = audio.content();
            log.info(
                    "Gemini transcription started model={} mime={} byteSize={}",
                    model,
                    mimeType,
                    bytes.length
            );
            Content content = dedicatedTranscribeModel()
                    ? Content.fromParts(Part.fromBytes(bytes, mimeType.toString()))
                    : Content.fromParts(
                            Part.fromText(USER_PROMPT),
                            Part.fromBytes(bytes, mimeType.toString()));
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

    private GenerateContentConfig config() {
        GenerateContentConfig.Builder builder = GenerateContentConfig.builder();
        if (!dedicatedTranscribeModel()) {
            builder.thinkingConfig(ThinkingConfig.builder()
                    .thinkingLevel(new ThinkingLevel("MINIMAL"))
                    .includeThoughts(false)
                    .build());
        }
        return builder.build();
    }

    private boolean dedicatedTranscribeModel() {
        return model != null && model.toLowerCase(Locale.ROOT).contains("transcribe");
    }

    static MimeType mimeType(String contentType, String filename) {
        String type = mediaType(contentType);
        if (type != null) {
            return MimeType.valueOf(type);
        }
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (name.endsWith(".mp3") || name.endsWith(".mpeg")) {
            return MimeType.valueOf("audio/mpeg");
        }
        if (name.endsWith(".mp4") || name.endsWith(".m4a")) {
            return MimeType.valueOf("audio/mp4");
        }
        if (name.endsWith(".wav")) {
            return MimeType.valueOf("audio/wav");
        }
        if (name.endsWith(".ogg")) {
            return MimeType.valueOf("audio/ogg");
        }
        return MimeType.valueOf("audio/webm");
    }

    private static String mediaType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return null;
        }
        String type = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (type.isEmpty() || "application/octet-stream".equals(type)) {
            return null;
        }
        return type;
    }
}
