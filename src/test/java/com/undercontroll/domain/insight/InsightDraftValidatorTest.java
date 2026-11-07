package com.undercontroll.domain.insight;

import com.undercontroll.domain.model.insight.InsightDraft;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.undercontroll.domain.insight.InsightCandidates.BRAND;
import static com.undercontroll.domain.insight.InsightCandidates.PRICE;
import static com.undercontroll.domain.insight.InsightCandidates.RISING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsightDraftValidatorTest {

    private static final InsightContract CONTRACT = new InsightContract(2);
    private static final String PRICE_TEXT = "No {{mes_atual}} os preços de {{categoria}} caíram {{variacao_preco_pct}} "
            + "frente ao {{mes_comparacao}}, nos mesmos produtos. Peças ficam mais baratas para a oficina.";
    private static final String ACTION = "Renegociar o preço das peças de reposição.";

    @Test
    @DisplayName("a draft that only references candidate placeholders is valid")
    void valid() {
        assertEquals(List.of(), validate(new InsightDraft.Item(PRICE.id(), PRICE_TEXT, ACTION)));
        assertEquals(List.of(), InsightDraftValidator.validate(new InsightDraft(List.of()), List.of(), CONTRACT));
    }

    @Test
    @DisplayName("unknown, repeated and excess candidates are reported with their position")
    void references() {
        List<String> errors = validate(
                new InsightDraft.Item("ALERTA_PRECO:MLB-FANS", PRICE_TEXT, ACTION),
                new InsightDraft.Item(PRICE.id(), PRICE_TEXT, ACTION),
                new InsightDraft.Item(PRICE.id(), PRICE_TEXT, ACTION),
                null
        );

        assertTrue(errors.contains("insights has 4 items; the maximum is 2"));
        assertTrue(errors.contains("insights[0].candidate_id ALERTA_PRECO:MLB-FANS is not one of the candidates"));
        assertTrue(errors.contains("insights[2].candidate_id ALERTA_PRECO:MLB-MICROWAVES is repeated"));
        assertTrue(errors.contains("insights[3] is empty"));
        assertEquals(List.of("draft is empty"), InsightDraftValidator.validate(null, List.of(PRICE), CONTRACT));
    }

    @Test
    @DisplayName("numbers must come from the candidate's own placeholders")
    void numbers() {
        List<String> foreign = validate(new InsightDraft.Item(RISING.id(),
                "{{assunto}} subiu e custa {{mediana_atual}} na mediana, sinal relevante para a oficina que já conserta o modelo.",
                ACTION));
        List<String> literal = validate(new InsightDraft.Item(PRICE.id(),
                "Os preços de {{categoria}} caíram 6,5% no mês, sinal de peças mais baratas para a oficina.", ACTION));

        assertEquals(1, foreign.size());
        assertTrue(foreign.getFirst().startsWith("insights[0].text uses {{mediana_atual}}, which candidate TENDENCIA_ALTA:MLB1"));
        assertTrue(literal.contains(
                "insights[0].text writes digits directly; every number, month or product name must be a placeholder"));
    }

    @Test
    @DisplayName("sales claims, jargon, missing text and length are rejected")
    void language() {
        List<String> errors = validate(
                new InsightDraft.Item(PRICE.id(), "{{categoria}} vendeu muito no bucket do mês, com rank alto e boa margem de lucro.", ""),
                new InsightDraft.Item(BRAND.id(), "{{assunto}} cresceu.", "x".repeat(InsightContract.MAX_ACTION_LENGTH + 1))
        );

        assertTrue(errors.contains("insights[0].text claims sales volume; the data measures popularity and listed prices"));
        assertTrue(errors.contains("insights[0].text uses internal jargon (bucket, rank, score, ids or view names)"));
        assertTrue(errors.contains("insights[0].action is required"));
        assertTrue(errors.stream().anyMatch(error -> error.startsWith("insights[1].text has 16 characters")));
        assertTrue(errors.stream().anyMatch(error -> error.startsWith("insights[1].action has 221 characters")));
    }

    private static List<String> validate(InsightDraft.Item... items) {
        return InsightDraftValidator.validate(new InsightDraft(new ArrayList<>(Arrays.asList(items))),
                List.of(PRICE, RISING, BRAND), CONTRACT);
    }
}
