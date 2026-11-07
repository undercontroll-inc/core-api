package com.undercontroll.infrastructure.ai.config;

import java.util.Locale;
import java.util.Set;

public record ModelCapabilities(ThinkingControl thinkingControl, boolean dedicatedTranscription) {

    public enum ThinkingControl {
        NONE(Set.of()),
        BUDGET(Set.of(ThinkingPolicy.OFF, ThinkingPolicy.LOW, ThinkingPolicy.HIGH)),
        LEVEL(Set.of(ThinkingPolicy.MINIMAL, ThinkingPolicy.LOW, ThinkingPolicy.HIGH)),
        REASONING_EFFORT(Set.of(ThinkingPolicy.MINIMAL, ThinkingPolicy.LOW, ThinkingPolicy.HIGH)),
        UNKNOWN(Set.of());

        private final Set<ThinkingPolicy> supported;

        ThinkingControl(Set<ThinkingPolicy> supported) {
            this.supported = supported;
        }

        public boolean supports(ThinkingPolicy policy) {
            return supported.contains(policy);
        }
    }

    public static ModelCapabilities of(AiProvider provider, String model) {
        String name = model == null ? "" : model.trim().toLowerCase(Locale.ROOT);
        return switch (provider) {
            case NONE -> new ModelCapabilities(ThinkingControl.NONE, false);
            case OPENAI -> new ModelCapabilities(openAiControl(name), false);
            case GEMINI -> gemini(name);
        };
    }

    private static ThinkingControl openAiControl(String model) {
        if (model.startsWith("gpt-5") || model.matches("o[1-9].*")) {
            return ThinkingControl.REASONING_EFFORT;
        }
        return ThinkingControl.NONE;
    }

    private static ModelCapabilities gemini(String model) {
        if (model.contains("transcribe")) {
            return new ModelCapabilities(ThinkingControl.NONE, true);
        }
        if (model.startsWith("gemini-2.5")) {
            return new ModelCapabilities(ThinkingControl.BUDGET, false);
        }
        if (model.startsWith("gemini-3")) {
            return new ModelCapabilities(ThinkingControl.LEVEL, false);
        }
        return new ModelCapabilities(ThinkingControl.UNKNOWN, false);
    }
}
