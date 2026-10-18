package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.gateway.InsightsLlmGateway;
import com.undercontroll.infrastructure.config.InsightsProperties;
import com.undercontroll.infrastructure.mapper.InsightsPayloadJsonMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;

@AutoConfiguration(afterName = {
        "org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration"
})
@ConditionalOnBean(ChatModel.class)
public class InsightsChatClientConfig {

    @Bean(name = "insightsChatClient")
    ChatClient insightsChatClient(
            ChatModel chatModel,
            @Value("classpath:prompts/insights-system-prompt.txt") Resource systemPrompt,
            @Value("${spring.ai.model.chat:none}") String modelName,
            @Value("${spring.ai.google.genai.chat.options.model:gemini-2.5-flash}") String geminiModel
    ) {
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt, StandardCharsets.UTF_8)
                .defaultOptions(AiChatOptions.insights(modelName, geminiModel))
                .defaultTemplateRenderer(StTemplateRenderer.builder()
                        .startDelimiterToken('<')
                        .endDelimiterToken('>')
                        .build())
                .build();
    }

    @Bean
    InsightsLlmGateway insightsLlmGateway(
            ChatClient insightsChatClient,
            ObjectMapper objectMapper,
            InsightPayloadValidator insightPayloadValidator,
            InsightsProperties insightsProperties,
            MarketInsightTools marketInsightTools,
            InsightsPayloadJsonMapper payloadMapper,
            @Value("classpath:prompts/insights-user-prompt.txt") Resource userPrompt
    ) {
        return new SpringAiInsightsLlmAdapter(
                insightsChatClient,
                objectMapper,
                insightPayloadValidator,
                insightsProperties,
                userPrompt,
                marketInsightTools,
                payloadMapper
        );
    }

    @Bean
    InsightPayloadValidator insightPayloadValidator(InsightsProperties properties) {
        return new InsightPayloadValidator(properties.getMaxInsights());
    }
}
