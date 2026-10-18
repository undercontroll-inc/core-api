package com.undercontroll.application.controller.impl;

import com.undercontroll.application.controller.TranscriptionApi;
import com.undercontroll.application.dto.transcription.TranscribeAudioResponse;
import com.undercontroll.application.mapper.TranscriptionDtoMapper;
import com.undercontroll.domain.usecase.transcription.TranscribeAudioPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class TranscriptionController implements TranscriptionApi {

    private final TranscribeAudioPort transcribeAudioPort;
    private final TranscriptionDtoMapper transcriptionDtoMapper;

    @Override
    public ResponseEntity<TranscribeAudioResponse> transcribe(MultipartFile audio) {
        String text = transcribeAudioPort.execute(transcriptionDtoMapper.toClip(audio));
        return ResponseEntity.ok(transcriptionDtoMapper.toResponse(text));
    }
}
