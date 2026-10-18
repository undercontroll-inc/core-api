package com.undercontroll.application.dto.insights;

import com.undercontroll.domain.model.insight.InsightType;
import com.undercontroll.domain.model.insight.InsightsPayload;
import com.undercontroll.domain.model.market.MatchCoverage;

import java.util.List;
import java.util.Map;

public final class InsightsResponseMapper {

    private InsightsResponseMapper() {
    }

    public static MarketInsightsResponse from(InsightsPayload payload, String generatedAt) {
        if (payload == null) {
            return MarketInsightsResponse.empty();
        }
        InsightsPayload.Periodo periodo = payload.periodo();
        InsightsPayload.CoberturaMatch cobertura = payload.coberturaMatch();
        MarketInsightsResponse.MatchCoverageResponse match = cobertura == null
                ? MarketInsightsResponse.MatchCoverageResponse.empty()
                : new MarketInsightsResponse.MatchCoverageResponse(
                cobertura.nivel1Exato(),
                cobertura.nivel2MarcaCategoria(),
                cobertura.nivel3Categoria(),
                cobertura.semMatch()
        );
        List<MarketInsightsResponse.InsightItemResponse> insights = payload.insights() == null
                ? List.of()
                : payload.insights().stream().map(InsightsResponseMapper::mapInsight).toList();
        List<MarketInsightsResponse.RisingProductResponse> rising = payload.produtosEmAlta() == null
                ? List.of()
                : payload.produtosEmAlta().stream().map(InsightsResponseMapper::mapRising).toList();
        return new MarketInsightsResponse(
                periodo == null ? null : periodo.bucketAtual(),
                periodo == null ? null : periodo.bucketComparacao(),
                match,
                insights,
                rising,
                payload.limitacoes() == null ? List.of() : payload.limitacoes(),
                generatedAt
        );
    }

    public static MarketInsightsResponse.MatchCoverageResponse fromCoverage(MatchCoverage coverage) {
        if (coverage == null) {
            return MarketInsightsResponse.MatchCoverageResponse.empty();
        }
        return new MarketInsightsResponse.MatchCoverageResponse(
                coverage.exact(),
                coverage.brandCategory(),
                coverage.category(),
                coverage.none()
        );
    }

    private static MarketInsightsResponse.InsightItemResponse mapInsight(InsightsPayload.InsightItem item) {
        Map<String, Object> fields = item.evidencia() == null || item.evidencia().campos() == null
                ? Map.of()
                : item.evidencia().campos();
        String view = item.evidencia() == null ? null : item.evidencia().view();
        return new MarketInsightsResponse.InsightItemResponse(
                InsightType.normalize(item.tipo()),
                item.texto(),
                item.categoria(),
                item.categoriaNome(),
                new MarketInsightsResponse.EvidenceResponse(view, fields),
                item.confianca(),
                item.relacaoComConsertos(),
                item.acaoSugerida()
        );
    }

    private static MarketInsightsResponse.RisingProductResponse mapRising(InsightsPayload.ProdutoEmAlta product) {
        return new MarketInsightsResponse.RisingProductResponse(
                product.nome(),
                product.marca(),
                product.modelo(),
                product.categoria(),
                product.rank(),
                product.variacaoPosicao(),
                product.score(),
                product.precoMediano(),
                product.precosValidos(),
                product.confianca(),
                product.atendidoPeloCliente()
        );
    }
}
