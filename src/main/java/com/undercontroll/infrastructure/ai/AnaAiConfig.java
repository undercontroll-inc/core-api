package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.gateway.AnaChatGateway;
import com.undercontroll.infrastructure.ai.config.AiFeatureSettings;
import com.undercontroll.infrastructure.ai.config.ChatModelFactory;
import com.undercontroll.infrastructure.ai.config.ConditionalOnAiFeature;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
@ConditionalOnAiFeature("ana")
public class AnaAiConfig {

    @Bean
    AiFeatureSettings anaAiSettings(
            @Value("${undercontroll.ai.ana.provider:none}") String provider,
            @Value("${undercontroll.ai.ana.model:}") String model,
            @Value("${undercontroll.ai.ana.timeout:60s}") Duration timeout,
            @Value("${undercontroll.ai.ana.temperature:0.2}") Double temperature,
            @Value("${undercontroll.ai.ana.max-output-tokens:1024}") Integer maxOutputTokens,
            @Value("${undercontroll.ai.ana.thinking:default}") String thinking
    ) {
        return AiFeatureSettings.chat("ana", provider, model, timeout, temperature, maxOutputTokens, thinking);
    }

    @Bean
    ShopBriefingAdvisor shopBriefingAdvisor() {
        return new ShopBriefingAdvisor();
    }

    @Bean
    AnaWebSearchTool anaWebSearchTool(RestClient.Builder restClientBuilder) {
        return new AnaWebSearchTool(restClientBuilder);
    }

    @Bean
    BudgetedToolCallingManager anaToolCallingManager(
            ObjectMapper objectMapper,
            ObjectProvider<ObservationRegistry> observationRegistry,
            @Value("${undercontroll.ai.ana.max-tool-rounds:2}") int maxToolRounds,
            @Value("${undercontroll.ai.ana.max-web-searches:1}") int maxWebSearches
    ) {
        ToolCallingManager delegate = ToolCallingManager.builder()
                .observationRegistry(observationRegistry.getIfAvailable(() -> ObservationRegistry.NOOP))
                .build();
        return new BudgetedToolCallingManager(delegate, objectMapper, maxToolRounds, maxWebSearches);
    }

    @Bean(name = "anaChatClient")
    ChatClient anaChatClient(
            ChatModelFactory chatModelFactory,
            BudgetedToolCallingManager anaToolCallingManager,
            @Qualifier("anaAiSettings") AiFeatureSettings anaAiSettings,
            ShopBriefingAdvisor shopBriefingAdvisor,
            @Value("classpath:prompts/ana-system-prompt.txt") Resource systemPrompt
    ) {
        return ChatClient.builder(chatModelFactory.create(anaAiSettings, anaToolCallingManager))
                .defaultSystem(systemPrompt, StandardCharsets.UTF_8)
                .defaultAdvisors(shopBriefingAdvisor)
                .build();
    }

    @Bean
    AnaChatGateway anaChatGateway(
            @Qualifier("anaChatClient") ChatClient anaChatClient,
            AnaWebSearchTool anaWebSearchTool,
            AnaShopTools anaShopTools,
            @Value("${undercontroll.ai.ana.first-token-timeout:30s}") Duration firstTokenTimeout,
            @Value("${undercontroll.ai.ana.idle-timeout:20s}") Duration idleTimeout
    ) {
        return new SpringAiAnaChatAdapter(anaChatClient, anaWebSearchTool, anaShopTools, firstTokenTimeout, idleTimeout);
    }
}
