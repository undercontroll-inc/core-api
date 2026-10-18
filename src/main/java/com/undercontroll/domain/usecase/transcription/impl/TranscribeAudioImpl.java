package com.undercontroll.domain.usecase.transcription.impl;

import com.undercontroll.domain.exception.InvalidTranscriptionException;
import com.undercontroll.domain.exception.TranscriptionUnavailableException;
import com.undercontroll.domain.gateway.AudioTranscriptionGateway;
import com.undercontroll.domain.model.AudioClip;
import com.undercontroll.domain.support.Durations;
import com.undercontroll.domain.usecase.transcription.TranscribeAudioPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscribeAudioImpl implements TranscribeAudioPort {

    static final long MAX_BYTES = 8L * 1024 * 1024;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "audio/webm",
            "audio/mpeg",
            "audio/mp3",
            "audio/mp4",
            "audio/m4a",
            "audio/x-m4a",
            "audio/wav",
            "audio/x-wav",
            "audio/wave",
            "audio/ogg"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "webm", "mp3", "mpeg", "mp4", "m4a", "wav", "ogg"
    );

    private final ObjectProvider<AudioTranscriptionGateway> audioTranscriptionGateway;

    @Override
    public String execute(AudioClip audio) {
        validate(audio);
        long started = System.nanoTime();
        log.info("Transcription requested mime={} byteSize={}", audio.contentType(), audio.size());
        AudioTranscriptionGateway gateway = audioTranscriptionGateway.getIfAvailable();
        if (gateway == null) {
            log.warn("Transcription gateway is not available");
            throw new TranscriptionUnavailableException();
        }
        try {
            String text = gateway.transcribe(audio);
            if (isBlank(text)) {
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

    private static void validate(AudioClip audio) {
        if (audio == null || audio.content() == null) {
            throw new InvalidTranscriptionException("Audio file is required");
        }
        if (audio.size() <= 0) {
            throw new InvalidTranscriptionException("Audio file is empty");
        }
        if (audio.size() > MAX_BYTES) {
            throw new InvalidTranscriptionException("Audio file exceeds 8MB");
        }
        if (!allowed(audio.contentType(), audio.filename())) {
            throw new InvalidTranscriptionException("Unsupported audio type");
        }
    }

    static boolean allowed(String contentType, String filename) {
        String type = mediaType(contentType);
        if (type != null && ALLOWED_TYPES.contains(type)) {
            return true;
        }
        String extension = extension(filename);
        return extension != null && ALLOWED_EXTENSIONS.contains(extension);
    }

    private static String mediaType(String contentType) {
        if (isBlank(contentType)) {
            return null;
        }
        String type = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return type.isEmpty() || "application/octet-stream".equals(type) ? null : type;
    }

    private static String extension(String filename) {
        if (isBlank(filename)) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
