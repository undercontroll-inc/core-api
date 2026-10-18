package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.exception.TranscriptionUnavailableException;
import com.undercontroll.domain.gateway.AudioTranscriptionGateway;
import com.undercontroll.domain.model.AudioClip;
import com.undercontroll.domain.support.Durations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

@Slf4j
public class OpenAiAudioTranscriptionAdapter implements AudioTranscriptionGateway {

    static final String MODEL = "whisper-1";

    private final TranscriptionModel transcriptionModel;

    public OpenAiAudioTranscriptionAdapter(TranscriptionModel transcriptionModel) {
        this.transcriptionModel = transcriptionModel;
    }

    @Override
    public String transcribe(AudioClip audio) {
        long started = System.nanoTime();
        log.info("OpenAI transcription started model={} mime={} byteSize={}", MODEL, audio.contentType(), audio.size());
        try {
            var options = OpenAiAudioTranscriptionOptions.builder()
                    .language("pt")
                    .model(MODEL)
                    .temperature(0f)
                    .build();
            String text = transcriptionModel.call(new AudioTranscriptionPrompt(resource(audio), options))
                    .getResult()
                    .getOutput();
            if (!StringUtils.hasText(text)) {
                log.warn(
                        "OpenAI transcription returned empty text model={} durationMs={}",
                        MODEL,
                        Durations.millisSince(started)
                );
                throw new TranscriptionUnavailableException();
            }
            log.info(
                    "OpenAI transcription finished model={} chars={} durationMs={}",
                    MODEL,
                    text.trim().length(),
                    Durations.millisSince(started)
            );
            return text.trim();
        } catch (TranscriptionUnavailableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn(
                    "OpenAI transcription failed model={} durationMs={} cause={}",
                    MODEL,
                    Durations.millisSince(started),
                    ex.toString()
            );
            throw new TranscriptionUnavailableException(ex);
        }
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
