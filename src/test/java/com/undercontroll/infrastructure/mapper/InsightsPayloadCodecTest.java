package com.undercontroll.infrastructure.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.exception.InsightPayloadUnreadableException;
import com.undercontroll.domain.model.insight.InsightsPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsightsPayloadCodecTest {

    private final InsightsPayloadCodec codec = new InsightsPayloadCodec(new ObjectMapper(), new InsightsPayloadJsonMapperImpl());

    private static final String STORED = """
            {
              "periodo": { "bucket_atual": "2026-08", "bucket_comparacao": "2026-07" },
              "cobertura_match": { "nivel_1_exato": 1, "nivel_2_marca_categoria": 2, "nivel_3_categoria": 3, "sem_match": 4 },
              "insights": [{
                "texto": "Micro-ondas em alta", "tipo": "mix", "categoria": "MLB1", "categoria_nome": "Micro-ondas",
                "evidencia": { "view": "vw_market_category", "campos": { "rising": 3 } },
                "confianca": "alta", "relacao_com_consertos": "direta", "acao_sugerida": "estocar"
              }],
              "produtos_em_alta": [{ "nome": "MO 20L", "marca": "Mondial", "rank": 1, "ofertas": 9, "atendido_pelo_cliente": true }],
              "limitacoes": ["amostra"],
              "campo_novo": "ignorado"
            }
            """;

    @Test
    @DisplayName("decodes the stored snake_case payload into the domain record")
    void decodesStoredPayload() {
        InsightsPayload payload = codec.decode(STORED);

        assertEquals("2026-08", payload.periodo().bucketAtual());
        assertEquals(new InsightsPayload.CoberturaMatch(1, 2, 3, 4), payload.coberturaMatch());
        assertEquals("Micro-ondas", payload.insights().get(0).categoriaNome());
        assertEquals(Map.of("rising", 3), payload.insights().get(0).evidencia().campos());
        assertEquals("estocar", payload.insights().get(0).acaoSugerida());
        assertTrue(payload.produtosEmAlta().get(0).atendidoPeloCliente());
        assertEquals(List.of("amostra"), payload.limitacoes());
    }

    @Test
    @DisplayName("encodes with the same snake_case contract it reads")
    void roundTrip() {
        InsightsPayload payload = codec.decode(STORED);

        String json = codec.encode(payload);

        assertTrue(json.contains("\"cobertura_match\""));
        assertTrue(json.contains("\"nivel_2_marca_categoria\":2"));
        assertEquals(payload, codec.decode(json));
    }

    @Test
    @DisplayName("a missing payload is absent but an unreadable one is an error")
    void absentPayload() {
        assertNull(codec.decode(null));
        assertNull(codec.decode(" "));
        assertNull(codec.encode(null));
        assertThrows(InsightPayloadUnreadableException.class, () -> codec.decode("{not-json"));
    }
}
