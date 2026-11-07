package com.undercontroll.domain.insight;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.undercontroll.domain.insight.InsightCandidates.BRAND;
import static com.undercontroll.domain.insight.InsightCandidates.PRICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InsightContractTest {

    @Test
    @DisplayName("each candidate exposes common placeholders it can fill plus its own metrics")
    void placeholders() {
        assertEquals(List.of("assunto", "categoria", "mes_atual", "mes_comparacao", "variacao_preco_pct",
                "produtos_pareados", "mediana_atual"), List.copyOf(InsightContract.placeholders(PRICE)));
        assertEquals(Set.of("assunto", "mes_atual", "produtos_em_alta"), InsightContract.placeholders(BRAND));
    }

    @Test
    @DisplayName("max insights stays within the contract")
    void bounds() {
        assertThrows(IllegalArgumentException.class, () -> new InsightContract(0));
        assertThrows(IllegalArgumentException.class, () -> new InsightContract(InsightContract.MAX_INSIGHTS + 1));
        assertEquals(3, new InsightContract(3).maxInsights());
    }
}
