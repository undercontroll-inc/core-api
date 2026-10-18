package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.gateway.InsightsLlmGateway;
import com.undercontroll.domain.model.insight.InsightGenerationSettings;
import com.undercontroll.infrastructure.ai.config.AiFeatureSettings;
import com.undercontroll.infrastructure.ai.config.ChatModelFactory;
import com.undercontroll.infrastructure.ai.config.ConditionalOnAiFeature;
import com.undercontroll.domain.insight.InsightContract;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
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
            @Value("${undercontroll.ai.insights.prompt-version:7}") String promptVersion,
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
    InsightContract insightContract(InsightGenerationSettings settings) {
        return new InsightContract(settings.maxInsights());
    }

    @Bean
    @ConditionalOnAiFeature("insights")
    InsightsLlmGateway insightsLlmGateway(
            ChatModelFactory chatModelFactory,
            @Qualifier("insightsAiSettings") AiFeatureSettings insightsAiSettings,
            ObjectMapper objectMapper,
            InsightGenerationSettings settings,
            @Value("classpath:prompts/insights-system-prompt.txt") Resource systemPrompt,
            @Value("classpath:prompts/insights-user-prompt.txt") Resource userPrompt
    ) {
        ChatModel chatModel = chatModelFactory.create(insightsAiSettings);
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt, StandardCharsets.UTF_8)
                .defaultTemplateRenderer(StTemplateRenderer.builder()
                        .startDelimiterToken('<')
                        .endDelimiterToken('>')
                        .build())
                .build();
        return new SpringAiInsightsLlmAdapter(chatClient, chatModel.getDefaultOptions(), objectMapper, settings,
                userPrompt);
    }
}
