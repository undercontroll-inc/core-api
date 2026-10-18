package com.undercontroll.domain.usecase.transcription;

import com.undercontroll.domain.model.AudioClip;

public interface TranscribeAudioPort {

    String execute(AudioClip audio);
}
