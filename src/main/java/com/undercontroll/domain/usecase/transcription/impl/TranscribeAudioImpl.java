package com.undercontroll.domain.usecase.transcription.impl;

import com.undercontroll.domain.exception.InvalidTranscriptionException;
import com.undercontroll.domain.exception.TranscriptionUnavailableException;
import com.undercontroll.domain.gateway.AudioTranscriptionGateway;
import com.undercontroll.domain.model.AudioClip;
import com.undercontroll.domain.model.AudioFormat;
import com.undercontroll.domain.model.AudioFormatDetector;
import com.undercontroll.domain.support.Durations;
import com.undercontroll.domain.usecase.transcription.TranscribeAudioPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
public class TranscribeAudioImpl implements TranscribeAudioPort {

    static final long MAX_BYTES = 8L * 1024 * 1024;

    private final ObjectProvider<AudioTranscriptionGateway> audioTranscriptionGateway;
    private final Duration maxDuration;

    public TranscribeAudioImpl(
            ObjectProvider<AudioTranscriptionGateway> audioTranscriptionGateway,
            @Value("${undercontroll.ai.transcription.max-duration:180s}") Duration maxDuration
    ) {
        this.audioTranscriptionGateway = audioTranscriptionGateway;
        this.maxDuration = maxDuration;
    }

    @Override
    public String execute(AudioClip audio) {
        AudioClip clip = validate(audio);
        long started = System.nanoTime();
        log.info("Transcription requested format={} byteSize={}", clip.format(), clip.size());
        AudioTranscriptionGateway gateway = audioTranscriptionGateway.getIfAvailable();
        if (gateway == null) {
            log.warn("Transcription gateway is not available");
            throw new TranscriptionUnavailableException();
        }
        if (!gateway.supportedFormats().contains(clip.format())) {
            throw new InvalidTranscriptionException(
                    "Audio format " + clip.format().extension() + " is not supported by the transcription provider");
        }
        try {
            String text = gateway.transcribe(clip);
            if (text == null || text.isBlank()) {
                throw new TranscriptionUnavailableException();
            }
            String trimmed = text.trim();
            log.info(
                    "Transcription finished chars={} durationMs={}",
                    trimmed.length(),
                    Durations.millisSince(started)
            );
            return trimmed;
        } catch (TranscriptionUnavailableException | InvalidTranscriptionException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new TranscriptionUnavailableException(ex);
        }
    }

    AudioClip validate(AudioClip audio) {
        if (audio == null || audio.content() == null) {
            throw new InvalidTranscriptionException("Audio file is required");
        }
        if (audio.size() <= 0) {
            throw new InvalidTranscriptionException("Audio file is empty");
        }
        if (audio.size() > MAX_BYTES) {
            throw new InvalidTranscriptionException("Audio file exceeds 8MB");
        }
        AudioFormat detected = AudioFormatDetector.detect(audio.content())
                .orElseThrow(() -> new InvalidTranscriptionException(
                        "Unsupported or unrecognized audio content; use webm, ogg, wav, mp3 or m4a"));
        String declared = mediaType(audio.contentType());
        if (declared != null) {
            Optional<AudioFormat> declaredFormat = AudioFormat.fromMimeType(declared);
            if (declaredFormat.isEmpty()) {
                throw new InvalidTranscriptionException("Unsupported audio type " + declared);
            }
            if (declaredFormat.get() != detected) {
                throw new InvalidTranscriptionException("Declared type " + declared
                        + " does not match the audio content (" + detected.extension() + ")");
            }
        }
        Optional<Duration> duration = AudioFormatDetector.wavDuration(audio.content());
        if (duration.isPresent() && duration.get().compareTo(maxDuration) > 0) {
            throw new InvalidTranscriptionException("Audio is longer than " + maxDuration.toSeconds() + " seconds");
        }
        return audio.normalized(detected);
    }

    private static String mediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        String type = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return type.isEmpty() || "application/octet-stream".equals(type) ? null : type;
    }
}
