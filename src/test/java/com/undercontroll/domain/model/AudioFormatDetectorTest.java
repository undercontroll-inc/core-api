package com.undercontroll.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AudioFormatDetectorTest {

    @ParameterizedTest(name = "{0} is detected as {1}")
    @CsvSource({
            "note.webm, WEBM",
            "note.ogg, OGG",
            "note.wav, WAV",
            "note.mp3, MP3",
            "note.m4a, M4A"
    })
    @DisplayName("detects real recordings by their magic bytes")
    void detectsRealFixtures(String fixture, AudioFormat expected) throws IOException {
        assertThat(AudioFormatDetector.detect(fixture(fixture))).contains(expected);
    }

    @Test
    @DisplayName("reads WAV duration by walking RIFF chunks, including a LIST chunk before data")
    void wavDuration() throws IOException {
        Optional<Duration> duration = AudioFormatDetector.wavDuration(fixture("note.wav"));

        assertThat(duration).isPresent();
        assertThat(duration.get().toMillis()).isBetween(500L, 560L);
        assertThat(AudioFormatDetector.wavDuration(fixture("note.mp3"))).isEmpty();
    }

    @Test
    @DisplayName("WAV without a data chunk or byte rate has no known duration")
    void wavWithoutData() {
        byte[] header = wav(0, false);
        byte[] zeroRate = wav(0, true);

        assertThat(AudioFormatDetector.detect(header)).contains(AudioFormat.WAV);
        assertThat(AudioFormatDetector.wavDuration(header)).isEmpty();
        assertThat(AudioFormatDetector.wavDuration(zeroRate)).isEmpty();
        assertThat(AudioFormatDetector.wavDuration(wav(16000, true))).contains(Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("rejects text, short input and Matroska that is not WebM")
    void rejectsUnknown() {
        byte[] matroska = {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, 'm', 'a', 't', 'r', 'o', 's', 'k', 'a'};

        assertThat(AudioFormatDetector.detect("hello world".getBytes(StandardCharsets.US_ASCII))).isEmpty();
        assertThat(AudioFormatDetector.detect(new byte[]{1, 2})).isEmpty();
        assertThat(AudioFormatDetector.detect(null)).isEmpty();
        assertThat(AudioFormatDetector.detect(matroska)).isEmpty();
        assertThat(AudioFormatDetector.detect("RIFFxxxxAVI ".getBytes(StandardCharsets.US_ASCII))).isEmpty();
    }

    @Test
    @DisplayName("an MP3 without ID3 tag is detected by its frame sync header (synthetic header)")
    void mp3FrameSync() {
        assertThat(AudioFormatDetector.detect(new byte[]{(byte) 0xFF, (byte) 0xFB, (byte) 0x90, 0x64})).contains(AudioFormat.MP3);
        assertThat(AudioFormatDetector.detect(new byte[]{(byte) 0xFF, (byte) 0xE0, 0, 0})).isEmpty();
    }

    @Test
    @DisplayName("maps MIME aliases to one canonical format")
    void mimeAliases() {
        assertThat(AudioFormat.fromMimeType("audio/x-wav")).contains(AudioFormat.WAV);
        assertThat(AudioFormat.fromMimeType("audio/webm;codecs=opus")).contains(AudioFormat.WEBM);
        assertThat(AudioFormat.fromMimeType("AUDIO/MP3")).contains(AudioFormat.MP3);
        assertThat(AudioFormat.fromMimeType("audio/x-m4a")).contains(AudioFormat.M4A);
        assertThat(AudioFormat.fromMimeType("audio/opus")).contains(AudioFormat.OGG);
        assertThat(AudioFormat.fromMimeType("text/plain")).isEmpty();
        assertThat(AudioFormat.fromMimeType(null)).isEmpty();
    }

    @Test
    @DisplayName("normalized clip carries the canonical MIME and extension of the detected format")
    void normalizedClip() {
        AudioClip clip = new AudioClip(new byte[]{1}, "text/plain", "C:\\gravacoes\\nota.webm").normalized(AudioFormat.M4A);

        assertThat(clip.contentType()).isEqualTo("audio/mp4");
        assertThat(clip.filename()).isEqualTo("nota.m4a");
        assertThat(clip.format()).isEqualTo(AudioFormat.M4A);
        assertThat(new AudioClip(new byte[]{1}, null, null).normalized(AudioFormat.WAV).filename()).isEqualTo("audio.wav");
        assertThat(new AudioClip(new byte[]{1}, null, ".hidden").normalized(AudioFormat.OGG).filename()).isEqualTo(".hidden.ogg");
    }

    static byte[] fixture(String name) throws IOException {
        try (InputStream in = AudioFormatDetectorTest.class.getResourceAsStream("/audio/" + name)) {
            return in.readAllBytes();
        }
    }

    private static byte[] wav(int byteRate, boolean withData) {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(withData ? 52 : 36).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        buffer.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(0).put("WAVE".getBytes(StandardCharsets.US_ASCII));
        buffer.put("fmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16)
                .putShort((short) 1).putShort((short) 1).putInt(8000).putInt(byteRate).putShort((short) 2).putShort((short) 16);
        if (withData) {
            buffer.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(16000).putLong(0);
        }
        return buffer.array();
    }
}
