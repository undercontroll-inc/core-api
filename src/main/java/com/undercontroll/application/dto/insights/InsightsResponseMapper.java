package com.undercontroll.application.dto.insights;

import com.undercontroll.domain.model.insight.InsightMonthLabel;
import com.undercontroll.domain.model.insight.InsightType;
import com.undercontroll.domain.model.insight.InsightsPayload;
import com.undercontroll.domain.usecase.insights.GetMarketInsightsPort;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class InsightsResponseMapper {

    private InsightsResponseMapper() {
    }

    public static MarketInsightsResponse from(GetMarketInsightsPort.Output output) {
        InsightsPayload payload = output.payload();
        InsightsPayload.CoberturaMatch cobertura = payload == null ? null : payload.coberturaMatch();
        MarketInsightsResponse.MatchCoverageResponse match = cobertura == null
                ? MarketInsightsResponse.MatchCoverageResponse.empty()
                : new MarketInsightsResponse.MatchCoverageResponse(
                cobertura.nivel1Exato(),
                cobertura.nivel2MarcaCategoria(),
                cobertura.nivel3Categoria(),
                cobertura.semMatch()
        );
        return new MarketInsightsResponse(
                output.state().name(),
                output.requestedBucketKey(),
                month(output.requestedBucketKey()),
                instant(output.snapshotPublishedAt()),
                output.availableBucketKey(),
                month(output.availableBucketKey()),
                payload == null || payload.periodo() == null ? null : payload.periodo().bucketComparacao(),
                instant(output.generatedAt()),
                output.failureReason(),
                match,
                payload == null || payload.insights() == null
                        ? List.of()
                        : payload.insights().stream().map(InsightsResponseMapper::mapInsight).toList(),
                payload == null || payload.produtosEmAlta() == null
                        ? List.of()
                        : payload.produtosEmAlta().stream().map(InsightsResponseMapper::mapRising).toList(),
                payload == null || payload.limitacoes() == null ? List.of() : payload.limitacoes()
        );
    }

    private static String month(String bucketKey) {
        return bucketKey == null ? null : InsightMonthLabel.of(bucketKey);
    }

    private static String instant(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private static MarketInsightsResponse.InsightItemResponse mapInsight(InsightsPayload.InsightItem item) {
        Map<String, Object> fields = item.evidencia() == null || item.evidencia().campos() == null
                ? Map.of()
                : item.evidencia().campos();
        String view = item.evidencia() == null ? null : item.evidencia().view();
        return new MarketInsightsResponse.InsightItemResponse(
                item.candidateId(),
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
