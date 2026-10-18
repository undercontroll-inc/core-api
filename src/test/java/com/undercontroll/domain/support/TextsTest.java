package com.undercontroll.domain.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TextsTest {

    @Test
    @DisplayName("firstNonBlank skips null, empty and blank values")
    void firstNonBlankSkipsEmpty() {
        assertEquals("AQ.key", Texts.firstNonBlank("", "  ", "AQ.key"));
        assertEquals("from-env", Texts.firstNonBlank(null, "from-env"));
        assertNull(Texts.firstNonBlank(null, " "));
        assertNull(Texts.firstNonBlank((String[]) null));
    }

    @Test
    @DisplayName("stripAccents removes combining marks and keeps null")
    void stripAccents() {
        assertEquals("Geladeira nao gela", Texts.stripAccents("Geladeira não gela"));
        assertNull(Texts.stripAccents(null));
    }

    @Test
    @DisplayName("normalize lowercases, drops punctuation and collapses whitespace")
    void normalize() {
        assertEquals("pedido 12 esta pronto", Texts.normalize("  Pedido #12,   está PRONTO! "));
        assertEquals("", Texts.normalize(null));
    }

    @Test
    @DisplayName("blankToNull strips values and maps blanks to null")
    void blankToNull() {
        assertEquals("Ana", Texts.blankToNull("  Ana "));
        assertNull(Texts.blankToNull(" "));
        assertNull(Texts.blankToNull(null));
    }

    @Test
    @DisplayName("clip trims and cuts at the max length")
    void clip() {
        assertEquals("liquidificador", Texts.clip(" liquidificador ", 28));
        assertEquals("abc", Texts.clip("abc def", 4));
        assertNull(Texts.clip(null, 4));
    }
}
