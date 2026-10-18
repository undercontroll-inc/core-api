package com.undercontroll.domain.model.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnaWebSearchNeedTest {

    @Test
    @DisplayName("detects manual, recall and tip questions")
    void matches() {
        assertTrue(AnaWebSearchNeed.matches("Cadê o manual da airfryer?"));
        assertTrue(AnaWebSearchNeed.matches("Tem recall desse micro-ondas?"));
        assertTrue(AnaWebSearchNeed.matches("Me dá uma dica de limpeza"));
        assertTrue(AnaWebSearchNeed.matches("Como limpar a airfryer?"));
        assertFalse(AnaWebSearchNeed.matches("Quais consertos estão abertos?"));
        assertFalse(AnaWebSearchNeed.matches(null));
    }

    @Test
    @DisplayName("keeps web search on follow-ups in the same conversation")
    void matchesConversationHistory() {
        assertTrue(AnaWebSearchNeed.matchesConversation(
                "E o link?",
                List.of("Cadê o manual da airfryer?")));
        assertFalse(AnaWebSearchNeed.matchesConversation(
                "E o valor?",
                List.of("Me fala do pedido 12")));
        assertFalse(AnaWebSearchNeed.matchesConversation("Oi", List.of()));
        assertFalse(AnaWebSearchNeed.matchesConversation("Oi", null));
    }
}
