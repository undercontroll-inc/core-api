package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.gateway.LabelExtractionGateway;
import com.undercontroll.infrastructure.ai.config.AiFeatureSettings;
import com.undercontroll.infrastructure.ai.config.ChatModelFactory;
import com.undercontroll.infrastructure.ai.config.ConditionalOnAiFeature;
import com.undercontroll.infrastructure.metrics.AiMetrics;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
@ConditionalOnAiFeature("label-reading")
public class LabelReadingAiConfig {

    @Bean
    AiFeatureSettings labelReadingAiSettings(
            @Value("${undercontroll.ai.label-reading.provider:none}") String provider,
            @Value("${undercontroll.ai.label-reading.model:}") String model,
            @Value("${undercontroll.ai.label-reading.timeout:20s}") Duration timeout,
            @Value("${undercontroll.ai.label-reading.temperature:0.0}") Double temperature,
            @Value("${undercontroll.ai.label-reading.max-output-tokens:1024}") Integer maxOutputTokens,
            @Value("${undercontroll.ai.label-reading.thinking:default}") String thinking
    ) {
        return AiFeatureSettings.chat("label-reading", provider, model, timeout, temperature, maxOutputTokens, thinking);
    }

    @Bean
    LabelExtractionGateway labelExtractionGateway(
            ChatModelFactory chatModelFactory,
            AiMetrics aiMetrics,
            @Qualifier("labelReadingAiSettings") AiFeatureSettings settings,
            ObjectMapper objectMapper,
            @Value("classpath:prompts/label-reading-system-prompt.txt") Resource systemPrompt,
            @Value("classpath:prompts/label-reading-user-prompt.txt") Resource userPrompt
    ) {
        ChatModel chatModel = chatModelFactory.create(settings);
        ChatClient chatClient = ChatClient.builder(chatModel, chatModelFactory.observationRegistry(), null, null)
                .defaultSystem(systemPrompt, StandardCharsets.UTF_8)
                .build();
        return new SpringAiLabelExtractionAdapter(chatClient, chatModel.getDefaultOptions(), objectMapper, settings,
                userPrompt, aiMetrics.feature(settings));
    }
}
