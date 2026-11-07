package com.undercontroll.application.dto.insights;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.model.insight.InsightsPayload;
import com.undercontroll.domain.model.insight.InsightReadState;
import com.undercontroll.domain.usecase.insights.GetMarketInsightsPort;
import com.undercontroll.infrastructure.mapper.InsightsPayloadCodec;
import com.undercontroll.infrastructure.mapper.InsightsPayloadJsonMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsightsResponseMapperTest {

    private final InsightsPayloadCodec codec = new InsightsPayloadCodec(new ObjectMapper(), new InsightsPayloadJsonMapperImpl());

    @Test
    @DisplayName("maps a full AI payload to the API response")
    void mapsPayload() {
        InsightsPayload payload = new InsightsPayload(
                new InsightsPayload.Periodo("2026-08", "2026-07"),
                new InsightsPayload.CoberturaMatch(1, 2, 3, 4),
                List.of(new InsightsPayload.InsightItem(
                        "ALERTA_PRECO:MLB-MICROWAVES",
                        "Texto",
                        "ALERTA_PRECO",
                        "MLB-MICROWAVES",
                        "Micro-ondas",
                        new InsightsPayload.Evidencia("vw_market_price_movement", Map.of("price_delta_pct", 11.11)),
                        "HIGH",
                        "NIVEL_1_EXATO",
                        "Reajustar"
                )),
                List.of(new InsightsPayload.ProdutoEmAlta(
                        "Micro-ondas Electrolux",
                        "Electrolux",
                        "ME23S",
                        "Micro-ondas",
                        2,
                        4,
                        81.3,
                        599.0,
                        18,
                        "HIGH",
                        true
                )),
                List.of("limitação")
        );

        MarketInsightsResponse response = InsightsResponseMapper.from(new GetMarketInsightsPort.Output(
                InsightReadState.STALE, "2026-09", Instant.parse("2026-10-01T03:00:00Z"), "2026-08",
                Instant.parse("2026-09-01T04:00:00Z"), null, payload));

        assertEquals("STALE", response.state());
        assertEquals("2026-09", response.requestedBucketKey());
        assertEquals("mês de setembro de 2026", response.requestedMonth());
        assertEquals("2026-10-01T03:00:00Z", response.snapshotPublishedAt());
        assertEquals("2026-08", response.availableBucketKey());
        assertEquals("mês de agosto de 2026", response.availableMonth());
        assertEquals("2026-07", response.comparisonBucketKey());
        assertEquals("ALERTA_PRECO:MLB-MICROWAVES", response.insights().get(0).id());
        assertEquals(1, response.matchCoverage().exact());
        assertEquals("ALERTA_PRECO", response.insights().get(0).type());
        assertEquals("Texto", response.insights().get(0).text());
        assertEquals("Electrolux", response.risingProducts().get(0).brand());
        assertEquals(18, response.risingProducts().get(0).validPriceCount());
        assertEquals("limitação", response.limitations().get(0));
        assertEquals("2026-09-01T04:00:00Z", response.generatedAt());
    }

    @Test
    @DisplayName("translates legacy insight types and ignores titulo")
    void mapsLegacyTypesAndDropsTitle() throws Exception {
        String json = """
                {
                  "periodo": { "bucket_atual": "2026-08", "bucket_comparacao": "2026-07" },
                  "cobertura_match": { "nivel_1_exato": 1, "nivel_2_marca_categoria": 0, "nivel_3_categoria": 0, "sem_match": 0 },
                  "insights": [
                    {
                      "titulo": "Alta de preço",
                      "texto": "Texto legado",
                      "tipo": "PRECO_ALTA",
                      "categoria": "MLB-MICROWAVES",
                      "categoria_nome": "Micro-ondas",
                      "evidencia": { "view": "vw_market_price_movement", "campos": { "price_delta_pct": 11.11 } },
                      "confianca": "HIGH",
                      "relacao_com_consertos": "NIVEL_1_EXATO",
                      "acao_sugerida": "Reajustar"
                    },
                    { "texto": "a", "tipo": "PRECO_QUEDA" },
                    { "texto": "b", "tipo": "DISPERSAO_PRECO" },
                    { "texto": "c", "tipo": "OPORTUNIDADE_ESTOQUE" },
                    { "texto": "d", "tipo": "PRODUTO_ALTA" },
                    { "texto": "e", "tipo": "MARCA" },
                    { "texto": "f", "tipo": "CONCENTRACAO_CONCORRENCIA" },
                    { "texto": "g", "tipo": "PERFIL_TECNICO" },
                    { "texto": "h", "tipo": "CATEGORIA_NAO_ATENDIDA" },
                    { "texto": "i", "tipo": "DESTAQUE_MARCA" }
                  ],
                  "produtos_em_alta": [],
                  "limitacoes": ["limitação"]
                }
                """;
        InsightsPayload payload = codec.decode(json);
        MarketInsightsResponse response = InsightsResponseMapper.from(ready(payload));

        assertEquals("ALERTA_PRECO", response.insights().get(0).type());
        assertEquals("Texto legado", response.insights().get(0).text());
        assertEquals("ALERTA_PRECO", response.insights().get(1).type());
        assertEquals("ALERTA_PRECO", response.insights().get(2).type());
        assertEquals("OPORTUNIDADE_ESTOQUE", response.insights().get(3).type());
        assertEquals("TENDENCIA_ALTA", response.insights().get(4).type());
        assertEquals("DESTAQUE_MARCA", response.insights().get(5).type());
        assertEquals("ALERTA_MERCADO", response.insights().get(6).type());
        assertEquals("OPORTUNIDADE_REPARO", response.insights().get(7).type());
        assertEquals("OPORTUNIDADE_REPARO", response.insights().get(8).type());
        assertEquals("DESTAQUE_MARCA", response.insights().get(9).type());
    }

    @Test
    @DisplayName("awaiting data has a state, no months and empty lists")
    void awaitingData() {
        MarketInsightsResponse response = InsightsResponseMapper.from(GetMarketInsightsPort.Output.awaitingData());

        assertEquals("AWAITING_DATA", response.state());
        assertNull(response.requestedBucketKey());
        assertNull(response.requestedMonth());
        assertNull(response.availableMonth());
        assertNull(response.generatedAt());
        assertNull(response.comparisonBucketKey());
        assertTrue(response.insights().isEmpty());
        assertEquals(MarketInsightsResponse.MatchCoverageResponse.empty(), response.matchCoverage());
    }

    @Test
    @DisplayName("maps coverage and handles missing nested collections")
    void partialPayload() {
        InsightsPayload payload = new InsightsPayload(
                null,
                null,
                null,
                null,
                null
        );
        MarketInsightsResponse response = InsightsResponseMapper.from(ready(payload));
        assertEquals(0, response.matchCoverage().exact());
        assertNull(response.comparisonBucketKey());
        assertTrue(response.insights().isEmpty());
        assertTrue(response.risingProducts().isEmpty());
        assertTrue(response.limitations().isEmpty());
    }

    private static GetMarketInsightsPort.Output ready(InsightsPayload payload) {
        return new GetMarketInsightsPort.Output(InsightReadState.READY, "2026-08", null, "2026-08", null, null, payload);
    }
}
