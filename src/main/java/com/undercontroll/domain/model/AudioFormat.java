package com.undercontroll.domain.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum AudioFormat {

    WEBM("audio/webm", "webm", Set.of("audio/webm", "video/webm")),
    OGG("audio/ogg", "ogg", Set.of("audio/ogg", "audio/opus", "application/ogg", "audio/x-ogg")),
    WAV("audio/wav", "wav", Set.of("audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave", "audio/x-pn-wav")),
    MP3("audio/mpeg", "mp3", Set.of("audio/mpeg", "audio/mp3", "audio/mpeg3", "audio/x-mpeg", "audio/x-mp3")),
    M4A("audio/mp4", "m4a", Set.of("audio/mp4", "audio/m4a", "audio/x-m4a", "video/mp4", "audio/aac"));

    private final String mimeType;
    private final String extension;
    private final Set<String> aliases;

    AudioFormat(String mimeType, String extension, Set<String> aliases) {
        this.mimeType = mimeType;
        this.extension = extension;
        this.aliases = aliases;
    }

    public String mimeType() {
        return mimeType;
    }

    public String extension() {
        return extension;
    }

    public static Optional<AudioFormat> fromMimeType(String mediaType) {
        if (mediaType == null) {
            return Optional.empty();
        }
        String normalized = mediaType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(format -> format.aliases.contains(normalized)).findFirst();
    }
}
