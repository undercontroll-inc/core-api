package com.undercontroll.domain.stockimport;

import com.undercontroll.domain.enums.StockImportMatchType;
import com.undercontroll.domain.gateway.ComponentAliasGateway;
import com.undercontroll.domain.gateway.ComponentGateway;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.stockimport.ComponentMatch;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComponentMatcherTest {

    private final ComponentGateway components = mock(ComponentGateway.class);
    private final ComponentAliasGateway aliases = mock(ComponentAliasGateway.class);
    private final ComponentMatcher matcher = new ComponentMatcher(components, aliases);

    private static ComponentPart component(int id, String name, String brand) {
        return ComponentPart.builder().id(id).name(name).brand(brand).quantity(5L).price(10.0).build();
    }

    @Test
    @DisplayName("learned alias wins with confidence 1.0")
    void aliasMatch() {
        ComponentPart motor = component(7, "Motor 220V", "Arno");
        when(aliases.find("cnpj:12345678000195", "code:mot-liq-220")).thenReturn(Optional.of(7));
        when(components.findById(7)).thenReturn(Optional.of(motor));
        when(components.searchByName(anyString(), anyInt())).thenReturn(List.of());

        ComponentMatch match = matcher.match("cnpj:12345678000195", "MOT-LIQ-220", "Motor liquidificador 220V");

        assertEquals(StockImportMatchType.ALIAS, match.type());
        assertEquals(7, match.componentId());
        assertEquals(1.0, match.confidence());
        assertEquals(7, match.candidates().getFirst().componentId());
    }

    @Test
    @DisplayName("fuzzy match above threshold suggests the component, ignoring accents and case")
    void fuzzyMatch() {
        when(aliases.find(anyString(), anyString())).thenReturn(Optional.empty());
        when(components.searchByName(anyString(), anyInt())).thenReturn(List.of(
                component(1, "Resistência 10k Ohm", null),
                component(2, "Capacitor 10uF", null)));

        ComponentMatch match = matcher.match("cnpj:1", null, "RESISTENCIA 10K OHM");

        assertEquals(StockImportMatchType.FUZZY, match.type());
        assertEquals(1, match.componentId());
        assertTrue(match.confidence() >= ComponentMatcher.AUTO_MATCH_SCORE);
        assertEquals(1, match.candidates().size());
    }

    @Test
    @DisplayName("weak similarity returns candidates without a suggestion")
    void weakMatch() {
        when(components.searchByName(anyString(), anyInt())).thenReturn(List.of(
                component(3, "Motor liquidificador 127V", "Mondial")));

        ComponentMatch match = matcher.match(null, null, "Motor liquidificador 220V");

        assertEquals(StockImportMatchType.NONE, match.type());
        assertNull(match.componentId());
        assertEquals(1, match.candidates().size());
    }

    @Test
    @DisplayName("score is symmetric token overlap with prefix credit")
    void score() {
        assertEquals(1.0, ComponentMatcher.score(List.of("motor", "220v"), List.of("220v", "motor")));
        assertEquals(0.0, ComponentMatcher.score(List.of("motor"), List.of("capacitor")));
        assertEquals(0.8, ComponentMatcher.score(List.of("resist"), List.of("resistencia")));
    }
}
