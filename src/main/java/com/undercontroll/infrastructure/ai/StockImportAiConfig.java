package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.gateway.InvoiceExtractionGateway;
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
@ConditionalOnAiFeature("stock-import")
public class StockImportAiConfig {

    @Bean
    AiFeatureSettings stockImportAiSettings(
            @Value("${undercontroll.ai.stock-import.provider:none}") String provider,
            @Value("${undercontroll.ai.stock-import.model:}") String model,
            @Value("${undercontroll.ai.stock-import.timeout:60s}") Duration timeout,
            @Value("${undercontroll.ai.stock-import.temperature:0.0}") Double temperature,
            @Value("${undercontroll.ai.stock-import.max-output-tokens:16384}") Integer maxOutputTokens,
            @Value("${undercontroll.ai.stock-import.thinking:low}") String thinking
    ) {
        return AiFeatureSettings.chat("stock-import", provider, model, timeout, temperature, maxOutputTokens, thinking);
    }

    @Bean
    InvoiceExtractionGateway invoiceExtractionGateway(
            ChatModelFactory chatModelFactory,
            AiMetrics aiMetrics,
            @Qualifier("stockImportAiSettings") AiFeatureSettings settings,
            ObjectMapper objectMapper,
            @Value("classpath:prompts/stock-import-system-prompt.txt") Resource systemPrompt,
            @Value("classpath:prompts/stock-import-user-prompt.txt") Resource userPrompt
    ) {
        ChatModel chatModel = chatModelFactory.create(settings);
        ChatClient chatClient = ChatClient.builder(chatModel, chatModelFactory.observationRegistry(), null, null)
                .defaultSystem(systemPrompt, StandardCharsets.UTF_8)
                .build();
        return new SpringAiInvoiceExtractionAdapter(chatClient, chatModel.getDefaultOptions(), objectMapper, settings,
                userPrompt, aiMetrics.feature(settings));
    }
}
