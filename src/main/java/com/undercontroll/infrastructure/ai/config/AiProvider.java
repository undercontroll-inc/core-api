package com.undercontroll.infrastructure.ai.config;

import java.util.Locale;

public enum AiProvider {

    NONE(null, null),
    OPENAI("gpt-4.1-mini", "whisper-1"),
    GEMINI("gemini-3.6-flash", "gemini-3.5-flash-lite");

    private final String defaultChatModel;
    private final String defaultTranscriptionModel;

    AiProvider(String defaultChatModel, String defaultTranscriptionModel) {
        this.defaultChatModel = defaultChatModel;
        this.defaultTranscriptionModel = defaultTranscriptionModel;
    }

    public static AiProvider parse(String feature, String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "none" -> NONE;
            case "openai" -> OPENAI;
            case "gemini", "google-genai", "google_genai" -> GEMINI;
            default -> throw new IllegalStateException(
                    "Invalid undercontroll.ai." + feature + ".provider='" + value
                            + "'. Use one of: none, openai, gemini");
        };
    }

    public String modelOrDefault(String configured, String geminiDefault, String openAiDefault) {
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return switch (this) {
            case GEMINI -> geminiDefault;
            case OPENAI -> openAiDefault;
            case NONE -> configured;
        };
    }

    public String defaultChatModel() {
        return defaultChatModel;
    }

    public String defaultTranscriptionModel() {
        return defaultTranscriptionModel;
    }

    public String label() {
        return name().toLowerCase(Locale.ROOT);
    }
}
