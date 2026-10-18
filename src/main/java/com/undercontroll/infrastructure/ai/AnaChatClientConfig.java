package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.gateway.AnaChatGateway;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@AutoConfiguration(afterName = {
        "org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration",
        "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration"
})
@ConditionalOnBean(ChatModel.class)
public class AnaChatClientConfig {

    @Bean
    ShopBriefingAdvisor shopBriefingAdvisor() {
        return new ShopBriefingAdvisor();
    }

    @Bean(name = "anaChatMemory")
    ChatMemory anaChatMemory(
            @Qualifier("anaChatMemoryRepository") ChatMemoryRepository chatMemoryRepository,
            @Value("${undercontroll.ana.memory-window:10}") int memoryWindow
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
            ChatModel chatModel,
            ShopBriefingAdvisor shopBriefingAdvisor,
            AnaChatMemoryAdvisor anaChatMemoryAdvisor,
            @Value("classpath:prompts/ana-system-prompt.txt") Resource systemPrompt,
            @Value("${spring.ai.model.chat:none}") String modelName,
            @Value("${spring.ai.google.genai.chat.options.model:gemini-2.5-flash}") String geminiModel
    ) {
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt, StandardCharsets.UTF_8)
                .defaultAdvisors(
                        shopBriefingAdvisor,
                        anaChatMemoryAdvisor
                )
                .defaultOptions(AiChatOptions.ana(modelName, geminiModel))
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
