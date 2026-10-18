package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.gateway.InsightsLlmGateway;
import com.undercontroll.domain.insight.InsightRetryPolicy;
import com.undercontroll.domain.model.insight.InsightGenerationSettings;
import com.undercontroll.domain.model.insight.InsightJobSettings;
import com.undercontroll.domain.support.Sleeper;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Random;
import java.util.random.RandomGenerator;

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
                repairCatalogDays,
                modelConfigHash(insightsAiSettings, maxInsights, repairCatalogDays)
        );
    }

    @Bean
    InsightJobSettings insightJobSettings(
            @Value("${undercontroll.ai.insights.job-deadline:5m}") Duration deadline,
            @Value("${undercontroll.ai.insights.lease-duration:10m}") Duration leaseDuration,
            @Value("${undercontroll.ai.insights.max-job-attempts:3}") int maxAttempts,
            @Value("${undercontroll.ai.insights.reschedule-delay:5m}") Duration rescheduleDelay
    ) {
        return new InsightJobSettings(deadline, leaseDuration, maxAttempts, rescheduleDelay);
    }

    @Bean
    InsightRetryPolicy insightRetryPolicy() {
        return InsightRetryPolicy.DEFAULT;
    }

    @Bean
    Sleeper insightRetrySleeper() {
        return duration -> Thread.sleep(duration.toMillis());
    }

    @Bean
    RandomGenerator insightRetryRandom() {
        return new Random();
    }

    static String modelConfigHash(AiFeatureSettings settings, int maxInsights, int repairCatalogDays) {
        String config = String.join("|",
                settings.provider().label(),
                String.valueOf(settings.model()),
                String.valueOf(settings.temperature()),
                String.valueOf(settings.maxOutputTokens()),
                settings.thinking().label(),
                InsightContract.VERSION,
                String.valueOf(maxInsights),
                String.valueOf(repairCatalogDays));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(config.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
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
