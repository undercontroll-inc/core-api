package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.gateway.AnaChatGateway;
import com.undercontroll.infrastructure.ai.config.AiFeatureSettings;
import com.undercontroll.infrastructure.ai.config.ChatModelFactory;
import com.undercontroll.infrastructure.ai.config.ConditionalOnAiFeature;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
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

    @Bean(name = "anaChatMemory")
    ChatMemory anaChatMemory(
            @Qualifier("anaChatMemoryRepository") ChatMemoryRepository chatMemoryRepository,
            @Value("${undercontroll.ai.ana.memory-window:10}") int memoryWindow
    ) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(memoryWindow)
                .build();
    }

    @Bean
    AnaChatMemoryAdvisor anaChatMemoryAdvisor(@Qualifier("anaChatMemory") ChatMemory anaChatMemory) {
        return new AnaChatMemoryAdvisor(anaChatMemory);
    }

    @Bean
    AnaWebSearchTool anaWebSearchTool(RestClient.Builder restClientBuilder) {
        return new AnaWebSearchTool(restClientBuilder);
    }

    @Bean(name = "anaChatClient")
    ChatClient anaChatClient(
            ChatModelFactory chatModelFactory,
            @Qualifier("anaAiSettings") AiFeatureSettings anaAiSettings,
            ShopBriefingAdvisor shopBriefingAdvisor,
            AnaChatMemoryAdvisor anaChatMemoryAdvisor,
            @Value("classpath:prompts/ana-system-prompt.txt") Resource systemPrompt
    ) {
        return ChatClient.builder(chatModelFactory.create(anaAiSettings))
                .defaultSystem(systemPrompt, StandardCharsets.UTF_8)
                .defaultAdvisors(shopBriefingAdvisor, anaChatMemoryAdvisor)
                .build();
    }

    @Bean
    AnaChatGateway anaChatGateway(
            @Qualifier("anaChatClient") ChatClient anaChatClient,
            @Qualifier("anaChatMemory") ChatMemory anaChatMemory,
            AnaWebSearchTool anaWebSearchTool,
            AnaShopTools anaShopTools
    ) {
        return new SpringAiAnaChatAdapter(anaChatClient, anaChatMemory, anaWebSearchTool, anaShopTools);
    }
}
