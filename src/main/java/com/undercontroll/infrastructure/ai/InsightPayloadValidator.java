package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.model.insight.InsightCandidate;
import com.undercontroll.domain.model.insight.InsightsPayload;
import com.undercontroll.domain.model.insight.InsightPromptContext;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class InsightPayloadValidator {

    static final int MIN_TEXT_LENGTH = 280;
    static final int MAX_TEXT_LENGTH = 1200;
    static final int MAX_ACTION_LENGTH = 280;

    private static final Pattern SALES_LANGUAGE = Pattern.compile(
            "vendeu\\s+\\d+|unidades vendidas|l[ií]der de vendas em unidades|volume de vendas",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern BUCKET_JARGON = Pattern.compile(
            "\\bbuckets?\\b|bucket[_\\s-]?key|bucket[_\\s-]?atual|bucket[_\\s-]?comparacao",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern SCHEMA_JARGON = Pattern.compile(
            "domain_id|vw_market_|get_repair_catalog|get_match_coverage|get_market_snapshot"
                    + "|list_price_movements|list_stock_opportunities|list_rising_products"
                    + "|list_brand_momentum|list_category_summary|list_price_dispersion"
                    + "|list_uncovered_categories|CONTEXTO_OBRIGATORIO",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern ISO_MONTH = Pattern.compile("\\b\\d{4}-\\d{2}\\b");

    private final int maxInsights;

    public InsightPayloadValidator(int maxInsights) {
        this.maxInsights = maxInsights;
    }

    public void validate(InsightsPayload payload, InsightPromptContext context) {
        validateHeader(payload, context);
        Set<String> candidates = new HashSet<>();
        for (InsightsPayload.InsightItem insight : payload.insights()) {
            if (insight.candidateId() != null && !candidates.add(insight.candidateId())) {
                throw new IllegalArgumentException("Duplicate candidate_id: " + insight.candidateId());
            }
            validateInsight(insight, context);
        }
        for (String limitation : payload.limitacoes()) {
            validateReadableLanguage(limitation);
        }
        if (payload.produtosEmAlta() != null) {
            for (InsightsPayload.ProdutoEmAlta product : payload.produtosEmAlta()) {
                validateSalesLanguage(product.nome());
                validateReadableLanguage(product.nome());
            }
        }
    }

    private void validateHeader(InsightsPayload payload, InsightPromptContext context) {
        if (payload == null) {
            throw new IllegalArgumentException("Insights payload is required");
        }
        if (payload.limitacoes() == null || payload.limitacoes().isEmpty()) {
            throw new IllegalArgumentException("limitacoes is required");
        }
        if (payload.insights() == null) {
            throw new IllegalArgumentException("insights is required");
        }
        if (payload.insights().size() > maxInsights) {
            throw new IllegalArgumentException("insights exceeds max of " + maxInsights);
        }
        if (payload.periodo() == null
                || !context.bucketKey().equals(payload.periodo().bucketAtual())) {
            throw new IllegalArgumentException("periodo.bucket_atual does not match the current bucket");
        }
        InsightsPayload.CoberturaMatch expected = InsightsPayload.CoberturaMatch.from(context.coverage());
        if (payload.coberturaMatch() == null || !payload.coberturaMatch().equals(expected)) {
            throw new IllegalArgumentException("cobertura_match must copy the grounded coverage");
        }
    }

    private void validateInsight(InsightsPayload.InsightItem insight, InsightPromptContext context) {
        InsightCandidate candidate = context.candidate(insight.candidateId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown candidate_id: " + insight.candidateId()));
        if (!candidate.type().name().equals(insight.tipo())) {
            throw new IllegalArgumentException("Insight tipo " + insight.tipo() + " does not match candidate "
                    + candidate.id());
        }
        int textLength = insight.texto() == null ? 0 : insight.texto().length();
        if (textLength < MIN_TEXT_LENGTH || textLength > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(
                    "Insight texto must be between " + MIN_TEXT_LENGTH + " and " + MAX_TEXT_LENGTH + " characters");
        }
        int actionLength = insight.acaoSugerida() == null ? 0 : insight.acaoSugerida().length();
        if (actionLength > MAX_ACTION_LENGTH) {
            throw new IllegalArgumentException("Insight acao_sugerida exceeds " + MAX_ACTION_LENGTH + " characters");
        }
        validateSalesLanguage(insight.texto());
        validateSalesLanguage(insight.acaoSugerida());
        validateReadableLanguage(insight.texto());
        validateReadableLanguage(insight.acaoSugerida());
    }

    private void validateSalesLanguage(String text) {
        if (text == null) {
            return;
        }
        if (SALES_LANGUAGE.matcher(text.toLowerCase(Locale.ROOT)).find()) {
            throw new IllegalArgumentException("Insight claims sales volume, which the ETL does not support");
        }
    }

    private void validateReadableLanguage(String text) {
        if (text == null) {
            return;
        }
        if (BUCKET_JARGON.matcher(text).find()
                || SCHEMA_JARGON.matcher(text).find()
                || ISO_MONTH.matcher(text).find()) {
            throw new IllegalArgumentException("Insight uses internal jargon (write mês de agosto de 2026, not 2026-08 or bucket)");
        }
    }
}
