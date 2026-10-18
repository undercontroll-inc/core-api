package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.undercontroll.domain.model.market.MatchCoverage;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InsightsPayloadJson(
        @JsonProperty("periodo") Periodo periodo,
        @JsonProperty("cobertura_match") CoberturaMatch coberturaMatch,
        @JsonProperty("insights") List<InsightItem> insights,
        @JsonProperty("produtos_em_alta") List<ProdutoEmAlta> produtosEmAlta,
        @JsonProperty("limitacoes") List<String> limitacoes
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Periodo(
            @JsonProperty("bucket_atual") String bucketAtual,
            @JsonProperty("bucket_comparacao") String bucketComparacao
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CoberturaMatch(
            @JsonProperty("nivel_1_exato") int nivel1Exato,
            @JsonProperty("nivel_2_marca_categoria") int nivel2MarcaCategoria,
            @JsonProperty("nivel_3_categoria") int nivel3Categoria,
            @JsonProperty("sem_match") int semMatch
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InsightItem(
            @JsonProperty("texto") String texto,
            @JsonProperty("tipo") String tipo,
            @JsonProperty("categoria") String categoria,
            @JsonProperty("categoria_nome") String categoriaNome,
            @JsonProperty("evidencia") Evidencia evidencia,
            @JsonProperty("confianca") String confianca,
            @JsonProperty("relacao_com_consertos") String relacaoComConsertos,
            @JsonProperty("acao_sugerida") String acaoSugerida
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Evidencia(
            @JsonProperty("view") String view,
            @JsonProperty("campos") Map<String, Object> campos
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProdutoEmAlta(
            @JsonProperty("nome") String nome,
            @JsonProperty("marca") String marca,
            @JsonProperty("modelo") String modelo,
            @JsonProperty("categoria") String categoria,
            @JsonProperty("rank") Integer rank,
            @JsonProperty("variacao_posicao") Integer variacaoPosicao,
            @JsonProperty("score") Double score,
            @JsonProperty("preco_mediano") Double precoMediano,
            @JsonProperty("variacao_preco_pct") Double variacaoPrecoPct,
            @JsonProperty("ofertas") Integer ofertas,
            @JsonProperty("confianca") String confianca,
            @JsonProperty("atendido_pelo_cliente") Boolean atendidoPeloCliente
    ) {
    }
}
