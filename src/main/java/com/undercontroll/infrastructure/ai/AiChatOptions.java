package com.undercontroll.infrastructure.ai;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel;

import java.util.Locale;

public final class AiChatOptions {

    static final int INSIGHTS_MAX_OUTPUT_TOKENS = 4096;
    static final String DEFAULT_GEMINI_MODEL = "gemini-2.5-flash";

    private AiChatOptions() {
    }

    public static ChatOptions ana(String provider) {
        return ana(provider, DEFAULT_GEMINI_MODEL);
    }

    public static ChatOptions ana(String provider, String geminiModel) {
        return of(provider, geminiModel, 0.2, 1024, false);
    }

    public static ChatOptions insights(String provider) {
        return insights(provider, DEFAULT_GEMINI_MODEL);
    }

    public static ChatOptions insights(String provider, String geminiModel) {
        return of(provider, geminiModel, 0.2, INSIGHTS_MAX_OUTPUT_TOKENS, true);
    }

    private static ChatOptions of(
            String provider,
            String geminiModel,
            double temperature,
            int maxTokens,
            boolean fast
    ) {
        if (isGemini(provider)) {
            GoogleGenAiChatOptions.Builder builder = GoogleGenAiChatOptions.builder()
                    .temperature(temperature)
                    .maxOutputTokens(maxTokens);
            if (fast) {
                applyFastThinking(builder, geminiModel);
            } else {
                applyAnaThinking(builder, geminiModel);
            }
            return builder.build();
        }
        return ChatOptions.builder()
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
    }

    private static void applyFastThinking(GoogleGenAiChatOptions.Builder builder, String geminiModel) {
        String model = geminiModel == null ? "" : geminiModel.toLowerCase(Locale.ROOT);
        if (model.contains("gemini-3")) {
            builder.thinkingLevel(GoogleGenAiThinkingLevel.LOW);
        } else if (model.contains("gemini-2.5")) {
            builder.thinkingBudget(0);
        }
    }

    private static void applyAnaThinking(GoogleGenAiChatOptions.Builder builder, String geminiModel) {
        String model = geminiModel == null ? "" : geminiModel.toLowerCase(Locale.ROOT);
        if (model.contains("gemini-2.5")) {
            builder.thinkingBudget(0);
        }
    }

    private static boolean isGemini(String chatModel) {
        return "google-genai".equals(chatModel) || "gemini".equals(chatModel);
    }
}
