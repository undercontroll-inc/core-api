package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.exception.TranscriptionUnavailableException;
import com.undercontroll.domain.gateway.AudioTranscriptionGateway;
import com.undercontroll.domain.model.AudioClip;
import com.undercontroll.domain.model.AudioFormat;
import com.undercontroll.domain.support.Durations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.Set;

@Slf4j
public class OpenAiAudioTranscriptionAdapter implements AudioTranscriptionGateway {

    static final Set<AudioFormat> SUPPORTED = Set.copyOf(EnumSet.allOf(AudioFormat.class));

    private final TranscriptionModel transcriptionModel;
    private final String model;

    public OpenAiAudioTranscriptionAdapter(TranscriptionModel transcriptionModel, String model) {
        this.transcriptionModel = transcriptionModel;
        this.model = model;
    }

    public static OpenAiAudioTranscriptionOptions options(String model) {
        return OpenAiAudioTranscriptionOptions.builder()
                .language("pt")
                .model(model)
                .temperature(0f)
                .build();
    }

    @Override
    public String transcribe(AudioClip audio) {
        long started = System.nanoTime();
        log.info("OpenAI transcription started model={} mime={} byteSize={}", model, audio.contentType(), audio.size());
        try {
            String text = transcriptionModel.call(new AudioTranscriptionPrompt(resource(audio), options(model)))
                    .getResult()
                    .getOutput();
            if (!StringUtils.hasText(text)) {
                log.warn(
                        "OpenAI transcription returned empty text model={} durationMs={}",
                        model,
                        Durations.millisSince(started)
                );
                throw new TranscriptionUnavailableException();
            }
            log.info(
                    "OpenAI transcription finished model={} chars={} durationMs={}",
                    model,
                    text.trim().length(),
                    Durations.millisSince(started)
            );
            return text.trim();
        } catch (TranscriptionUnavailableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn(
                    "OpenAI transcription failed model={} durationMs={} cause={}",
                    model,
                    Durations.millisSince(started),
                    ex.toString()
            );
            throw new TranscriptionUnavailableException(ex);
        }
    }

    @Override
    public Set<AudioFormat> supportedFormats() {
        return SUPPORTED;
    }

    static Resource resource(AudioClip audio) {
        return new ByteArrayResource(audio.content()) {
            @Override
            public String getFilename() {
                return audio.filename();
            }
        };
    }
}
