package com.undercontroll.domain.model;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

public final class AudioFormatDetector {

    private static final int EBML_DOCTYPE_WINDOW = 64;

    private AudioFormatDetector() {
    }

    public static Optional<AudioFormat> detect(byte[] content) {
        if (content == null || content.length < 4) {
            return Optional.empty();
        }
        if (startsWith(content, 0, "OggS")) {
            return Optional.of(AudioFormat.OGG);
        }
        if (startsWith(content, 0, "RIFF") && startsWith(content, 8, "WAVE")) {
            return Optional.of(AudioFormat.WAV);
        }
        if (startsWith(content, 4, "ftyp")) {
            return Optional.of(AudioFormat.M4A);
        }
        if (isWebm(content)) {
            return Optional.of(AudioFormat.WEBM);
        }
        if (startsWith(content, 0, "ID3") || isMpegFrame(content)) {
            return Optional.of(AudioFormat.MP3);
        }
        return Optional.empty();
    }

    public static Optional<Duration> wavDuration(byte[] content) {
        if (detect(content).filter(format -> format == AudioFormat.WAV).isEmpty()) {
            return Optional.empty();
        }
        long byteRate = 0;
        int offset = 12;
        while (offset + 8 <= content.length) {
            String chunk = new String(content, offset, 4, StandardCharsets.US_ASCII);
            long size = littleEndian(content, offset + 4);
            int body = offset + 8;
            if ("fmt ".equals(chunk) && body + 12 <= content.length) {
                byteRate = littleEndian(content, body + 8);
            } else if ("data".equals(chunk)) {
                if (byteRate <= 0) {
                    return Optional.empty();
                }
                return Optional.of(Duration.ofMillis(size * 1000 / byteRate));
            }
            offset = body + (int) Math.min(size + (size & 1), Integer.MAX_VALUE - body);
        }
        return Optional.empty();
    }

    private static boolean isWebm(byte[] content) {
        if ((content[0] & 0xFF) != 0x1A || (content[1] & 0xFF) != 0x45
                || (content[2] & 0xFF) != 0xDF || (content[3] & 0xFF) != 0xA3) {
            return false;
        }
        int window = Math.min(content.length, EBML_DOCTYPE_WINDOW);
        return new String(content, 0, window, StandardCharsets.US_ASCII).contains("webm");
    }

    private static boolean isMpegFrame(byte[] content) {
        return (content[0] & 0xFF) == 0xFF && (content[1] & 0xE0) == 0xE0 && (content[1] & 0x06) != 0;
    }

    private static boolean startsWith(byte[] content, int offset, String magic) {
        if (content.length < offset + magic.length()) {
            return false;
        }
        for (int i = 0; i < magic.length(); i++) {
            if (content[offset + i] != (byte) magic.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static long littleEndian(byte[] content, int offset) {
        return (content[offset] & 0xFFL)
                | (content[offset + 1] & 0xFFL) << 8
                | (content[offset + 2] & 0xFFL) << 16
                | (content[offset + 3] & 0xFFL) << 24;
    }
}
