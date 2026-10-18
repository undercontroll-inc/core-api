package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.AudioClip;
import com.undercontroll.domain.model.AudioFormat;

import java.util.Set;

public interface AudioTranscriptionGateway {

    String transcribe(AudioClip audio);

    Set<AudioFormat> supportedFormats();
}
