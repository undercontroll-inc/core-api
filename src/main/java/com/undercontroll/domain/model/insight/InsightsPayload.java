package com.undercontroll.domain.model.insight;

import com.undercontroll.domain.model.market.MatchCoverage;

import java.util.List;
import java.util.Map;

public record InsightsPayload(
        Periodo periodo,
        CoberturaMatch coberturaMatch,
        List<InsightItem> insights,
        List<ProdutoEmAlta> produtosEmAlta,
        List<String> limitacoes
) {
    public record Periodo(
            String bucketAtual,
            String bucketComparacao
    ) {
    }

    public record CoberturaMatch(
            int nivel1Exato,
            int nivel2MarcaCategoria,
            int nivel3Categoria,
            int semMatch
    ) {
        public static CoberturaMatch from(MatchCoverage coverage) {
            if (coverage == null) {
                return new CoberturaMatch(0, 0, 0, 0);
            }
            return new CoberturaMatch(
                    coverage.exact(),
                    coverage.brandCategory(),
                    coverage.category(),
                    coverage.none()
            );
        }
    }

    public record InsightItem(
            String texto,
            String tipo,
            String categoria,
            String categoriaNome,
            Evidencia evidencia,
            String confianca,
            String relacaoComConsertos,
            String acaoSugerida
    ) {
    }

    public record Evidencia(
            String view,
            Map<String, Object> campos
    ) {
    }

    public record ProdutoEmAlta(
            String nome,
            String marca,
            String modelo,
            String categoria,
            Integer rank,
            Integer variacaoPosicao,
            Double score,
            Double precoMediano,
            Double variacaoPrecoPct,
            Integer ofertas,
            String confianca,
            Boolean atendidoPeloCliente
    ) {
    }
}
