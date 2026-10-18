package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.gateway.AudioTranscriptionGateway;
import com.undercontroll.infrastructure.ai.config.AiFeatureSettings;
import com.undercontroll.infrastructure.ai.config.AiProvider;
import com.undercontroll.infrastructure.ai.config.ChatModelFactory;
import com.undercontroll.infrastructure.ai.config.ConditionalOnAiFeature;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConditionalOnAiFeature("transcription")
public class TranscriptionAiConfig {

    @Bean
    AiFeatureSettings transcriptionAiSettings(
            @Value("${undercontroll.ai.transcription.provider:none}") String provider,
            @Value("${undercontroll.ai.transcription.model:}") String model,
            @Value("${undercontroll.ai.transcription.timeout:60s}") Duration timeout,
            @Value("${undercontroll.ai.transcription.thinking:minimal}") String thinking
    ) {
        return AiFeatureSettings.transcription("transcription", provider, model, timeout, thinking);
    }

    @Bean
    AudioTranscriptionGateway audioTranscriptionGateway(
            ChatModelFactory chatModelFactory,
            @Qualifier("transcriptionAiSettings") AiFeatureSettings settings
    ) {
        if (settings.provider() == AiProvider.OPENAI) {
            OpenAiAudioTranscriptionModel model = new OpenAiAudioTranscriptionModel(
                    chatModelFactory.openAiAudioApi(settings),
                    OpenAiAudioTranscriptionAdapter.options(settings.model()),
                    ChatModelFactory.noRetry()
            );
            return new OpenAiAudioTranscriptionAdapter(model, settings.model());
        }
        return new GeminiAudioTranscriptionAdapter(chatModelFactory.geminiClient(settings), settings);
    }
}
