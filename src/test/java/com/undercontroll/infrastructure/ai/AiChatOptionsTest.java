package com.undercontroll.infrastructure.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class AiChatOptionsTest {

    @Test
    @DisplayName("Ana omits thinking level on Gemini 3")
    void anaOmitsThinkingOnGemini3() {
        GoogleGenAiChatOptions ana = assertInstanceOf(
                GoogleGenAiChatOptions.class,
                AiChatOptions.ana("google-genai", "gemini-3.6-flash")
        );
        assertNull(ana.getThinkingLevel());
        assertNull(ana.getThinkingBudget());
    }

    @Test
    @DisplayName("Ana disables thinking budget on Gemini 2.5")
    void anaDisablesThinkingOnGemini25() {
        GoogleGenAiChatOptions ana = assertInstanceOf(
                GoogleGenAiChatOptions.class,
                AiChatOptions.ana("google-genai", "gemini-2.5-flash")
        );
        assertEquals(0, ana.getThinkingBudget());
        assertNull(ana.getThinkingLevel());
    }

    @Test
    @DisplayName("insights uses fast thinking on Gemini 3")
    void insightsUsesFastThinkingOnGemini3() {
        GoogleGenAiChatOptions gemini = assertInstanceOf(
                GoogleGenAiChatOptions.class,
                AiChatOptions.insights("google-genai", "gemini-3.6-flash")
        );
        assertEquals(GoogleGenAiThinkingLevel.LOW, gemini.getThinkingLevel());
        assertEquals(4096, gemini.getMaxOutputTokens());
        assertNull(gemini.getThinkingBudget());
    }

    @Test
    @DisplayName("insights disables thinking budget on Gemini 2.5")
    void insightsDisablesThinkingOnGemini25() {
        ChatOptions options = AiChatOptions.insights("google-genai", "gemini-2.5-flash");
        GoogleGenAiChatOptions gemini = assertInstanceOf(GoogleGenAiChatOptions.class, options);
        assertEquals(0, gemini.getThinkingBudget());
        assertNull(gemini.getThinkingLevel());
    }
}
