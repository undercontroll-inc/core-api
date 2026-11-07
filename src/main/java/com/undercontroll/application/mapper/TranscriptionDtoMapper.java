package com.undercontroll.application.mapper;

import com.undercontroll.application.dto.transcription.TranscribeAudioResponse;
import com.undercontroll.domain.exception.TranscriptionUnavailableException;
import com.undercontroll.domain.model.AudioClip;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class TranscriptionDtoMapper {

    public AudioClip toClip(MultipartFile audio) {
        if (audio == null) {
            return null;
        }
        try {
            return new AudioClip(audio.getBytes(), audio.getContentType(), audio.getOriginalFilename());
        } catch (IOException ex) {
            throw new TranscriptionUnavailableException(ex);
        }
    }

    public TranscribeAudioResponse toResponse(String text) {
        return new TranscribeAudioResponse(text);
    }
}
