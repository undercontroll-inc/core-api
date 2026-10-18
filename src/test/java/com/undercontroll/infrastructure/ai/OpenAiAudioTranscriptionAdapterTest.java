package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.exception.TranscriptionUnavailableException;
import com.undercontroll.domain.model.AudioClip;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.transcription.TranscriptionModel;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAiAudioTranscriptionAdapterTest {

    @Mock
    private TranscriptionModel transcriptionModel;

    @Test
    @DisplayName("returns Whisper text")
    void transcribes() {
        AudioClip audio = new AudioClip("bytes".getBytes(), "audio/webm", "note.webm");
        when(transcriptionModel.call(any(AudioTranscriptionPrompt.class)))
                .thenReturn(new AudioTranscriptionResponse(new AudioTranscription("  Não gela ")));

        String text = new OpenAiAudioTranscriptionAdapter(transcriptionModel)
                .transcribe(audio);

        assertEquals("Não gela", text);
    }

    @Test
    @DisplayName("treats blank Whisper output as unavailable")
    void blankOutput() {
        when(transcriptionModel.call(any(AudioTranscriptionPrompt.class)))
                .thenReturn(new AudioTranscriptionResponse(new AudioTranscription(" ")));
        OpenAiAudioTranscriptionAdapter adapter = new OpenAiAudioTranscriptionAdapter(transcriptionModel);
        AudioClip audio = new AudioClip("bytes".getBytes(), "audio/webm", "note.webm");

        assertThrows(TranscriptionUnavailableException.class, () ->
                adapter.transcribe(audio));
    }

    @Test
    @DisplayName("wraps provider errors")
    void wrapsErrors() {
        when(transcriptionModel.call(any(AudioTranscriptionPrompt.class)))
                .thenThrow(new IllegalStateException("whisper down"));
        OpenAiAudioTranscriptionAdapter adapter = new OpenAiAudioTranscriptionAdapter(transcriptionModel);
        AudioClip audio = new AudioClip("bytes".getBytes(), "audio/webm", "note.webm");

        assertThrows(TranscriptionUnavailableException.class, () ->
                adapter.transcribe(audio));
    }

    @Test
    @DisplayName("sends the clip bytes with the original filename")
    void resourceKeepsFilename() throws Exception {
        var resource = OpenAiAudioTranscriptionAdapter.resource(new AudioClip("bytes".getBytes(), "audio/webm", "note.webm"));

        assertEquals("note.webm", resource.getFilename());
        assertArrayEquals("bytes".getBytes(), resource.getContentAsByteArray());
    }
}
