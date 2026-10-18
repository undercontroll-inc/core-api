package com.undercontroll.infrastructure.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public record AiFeatureSettings(
        String feature,
        AiProvider provider,
        String model,
        Duration timeout,
        Double temperature,
        Integer maxOutputTokens,
        ThinkingPolicy thinking,
        ModelCapabilities capabilities
) {

    private static final Logger log = LoggerFactory.getLogger(AiFeatureSettings.class);

    public static AiFeatureSettings chat(
            String feature,
            String provider,
            String model,
            Duration timeout,
            Double temperature,
            Integer maxOutputTokens,
            String thinking
    ) {
        AiProvider parsed = AiProvider.parse(feature, provider);
        return resolve(feature, parsed, model, parsed.defaultChatModel(), timeout, temperature, maxOutputTokens, thinking, true);
    }

    public static AiFeatureSettings transcription(
            String feature,
            String provider,
            String model,
            Duration timeout,
            String thinking
    ) {
        AiProvider parsed = AiProvider.parse(feature, provider);
        return resolve(feature, parsed, model, parsed.defaultTranscriptionModel(), timeout, null, null, thinking, false);
    }

    public boolean enabled() {
        return provider != AiProvider.NONE;
    }

    private static AiFeatureSettings resolve(
            String feature,
            AiProvider provider,
            String model,
            String defaultModel,
            Duration timeout,
            Double temperature,
            Integer maxOutputTokens,
            String thinking,
            boolean chat
    ) {
        ThinkingPolicy requested = ThinkingPolicy.parse(feature, thinking);
        if (provider == AiProvider.NONE) {
            return new AiFeatureSettings(feature, provider, null, timeout, temperature, maxOutputTokens,
                    ThinkingPolicy.DEFAULT, ModelCapabilities.of(provider, null));
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalStateException("undercontroll.ai." + feature + ".timeout must be positive");
        }
        if (maxOutputTokens != null && maxOutputTokens <= 0) {
            throw new IllegalStateException("undercontroll.ai." + feature + ".max-output-tokens must be positive");
        }
        String resolvedModel = model == null || model.isBlank() ? defaultModel : model.trim();
        ModelCapabilities capabilities = ModelCapabilities.of(provider, resolvedModel);
        ThinkingPolicy effective = effectiveThinking(feature, provider, resolvedModel, capabilities, requested, chat);
        return new AiFeatureSettings(feature, provider, resolvedModel, timeout, temperature, maxOutputTokens,
                effective, capabilities);
    }

    private static ThinkingPolicy effectiveThinking(
            String feature,
            AiProvider provider,
            String model,
            ModelCapabilities capabilities,
            ThinkingPolicy requested,
            boolean chat
    ) {
        if (requested == ThinkingPolicy.DEFAULT) {
            return requested;
        }
        ModelCapabilities.ThinkingControl control = capabilities.thinkingControl();
        if (control == ModelCapabilities.ThinkingControl.NONE) {
            log.info("AI feature {} ignores thinking={} because model {} has no thinking control",
                    feature, requested.label(), model);
            return ThinkingPolicy.DEFAULT;
        }
        boolean geminiChatMinimal = chat
                && provider == AiProvider.GEMINI
                && requested == ThinkingPolicy.MINIMAL;
        if (!control.supports(requested) || geminiChatMinimal) {
            throw new IllegalStateException("undercontroll.ai." + feature + ".thinking=" + requested.label()
                    + " is not supported by " + provider.label() + " model '" + model
                    + "'. Use thinking=default or a supported level");
        }
        return requested;
    }
}
