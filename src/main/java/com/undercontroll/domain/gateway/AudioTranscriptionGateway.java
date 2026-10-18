package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.AudioClip;

public interface AudioTranscriptionGateway {

    String transcribe(AudioClip audio);
}
