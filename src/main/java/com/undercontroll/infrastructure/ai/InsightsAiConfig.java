package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.gateway.InsightsLlmGateway;
import com.undercontroll.domain.model.insight.InsightGenerationSettings;
import com.undercontroll.infrastructure.ai.config.AiFeatureSettings;
import com.undercontroll.infrastructure.ai.config.ChatModelFactory;
import com.undercontroll.infrastructure.ai.config.ConditionalOnAiFeature;
import com.undercontroll.infrastructure.mapper.InsightsPayloadJsonMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
public class InsightsAiConfig {

    @Bean
    AiFeatureSettings insightsAiSettings(
            @Value("${undercontroll.ai.insights.provider:none}") String provider,
            @Value("${undercontroll.ai.insights.model:}") String model,
            @Value("${undercontroll.ai.insights.timeout:60s}") Duration timeout,
            @Value("${undercontroll.ai.insights.temperature:0.2}") Double temperature,
            @Value("${undercontroll.ai.insights.max-output-tokens:4096}") Integer maxOutputTokens,
            @Value("${undercontroll.ai.insights.thinking:low}") String thinking
    ) {
        return AiFeatureSettings.chat("insights", provider, model, timeout, temperature, maxOutputTokens, thinking);
    }

    @Bean
    InsightGenerationSettings insightGenerationSettings(
            @Qualifier("insightsAiSettings") AiFeatureSettings insightsAiSettings,
            @Value("${undercontroll.ai.insights.prompt-version:6}") String promptVersion,
            @Value("${undercontroll.ai.insights.max-insights:7}") int maxInsights,
            @Value("${undercontroll.ai.insights.repair-catalog-days:90}") int repairCatalogDays
    ) {
        return new InsightGenerationSettings(
                insightsAiSettings.provider().label(),
                insightsAiSettings.model(),
                promptVersion,
                maxInsights,
                repairCatalogDays
        );
    }

    @Bean
    InsightPayloadValidator insightPayloadValidator(InsightGenerationSettings settings) {
        return new InsightPayloadValidator(settings.maxInsights());
    }

    @Bean(name = "insightsChatClient")
    @ConditionalOnAiFeature("insights")
    ChatClient insightsChatClient(
            ChatModelFactory chatModelFactory,
            @Qualifier("insightsAiSettings") AiFeatureSettings insightsAiSettings,
            @Value("classpath:prompts/insights-system-prompt.txt") Resource systemPrompt
    ) {
        return ChatClient.builder(chatModelFactory.create(insightsAiSettings))
                .defaultSystem(systemPrompt, StandardCharsets.UTF_8)
                .defaultTemplateRenderer(StTemplateRenderer.builder()
                        .startDelimiterToken('<')
                        .endDelimiterToken('>')
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnAiFeature("insights")
    InsightsLlmGateway insightsLlmGateway(
            @Qualifier("insightsChatClient") ChatClient insightsChatClient,
            ObjectMapper objectMapper,
            InsightPayloadValidator insightPayloadValidator,
            InsightGenerationSettings settings,
            InsightsPayloadJsonMapper payloadMapper,
            @Value("classpath:prompts/insights-user-prompt.txt") Resource userPrompt
    ) {
        return new SpringAiInsightsLlmAdapter(
                insightsChatClient,
                objectMapper,
                insightPayloadValidator,
                settings,
                userPrompt,
                payloadMapper
        );
    }
}
