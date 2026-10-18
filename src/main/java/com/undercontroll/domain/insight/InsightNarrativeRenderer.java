package com.undercontroll.domain.insight;

import com.undercontroll.domain.model.insight.InsightCandidate;
import com.undercontroll.domain.model.insight.InsightDraft;
import com.undercontroll.domain.model.insight.InsightGenerationInput;
import com.undercontroll.domain.model.insight.InsightMetric;
import com.undercontroll.domain.model.insight.InsightMonthLabel;
import com.undercontroll.domain.model.insight.InsightsPayload;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

public final class InsightNarrativeRenderer {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private InsightNarrativeRenderer() {
    }

    public static InsightsPayload renderPayload(InsightDraft draft, InsightGenerationInput input) {
        List<InsightsPayload.InsightItem> insights = draft.insights().stream()
                .map(item -> {
                    InsightCandidate candidate = input.candidate(item.candidateId())
                            .orElseThrow(() -> new IllegalArgumentException("Unknown candidate " + item.candidateId()));
                    return new InsightsPayload.InsightItem(
                            candidate.id(),
                            render(item.text(), candidate),
                            candidate.type().name(),
                            candidate.domainId(),
                            candidate.categoryName(),
                            new InsightsPayload.Evidencia(candidate.evidenceView(), candidate.evidenceFields()),
                            candidate.confidence(),
                            candidate.repairRelation().name(),
                            render(item.action(), candidate)
                    );
                })
                .toList();
        return new InsightsPayload(
                new InsightsPayload.Periodo(input.bucketKey(), input.comparisonBucketKey()),
                InsightsPayload.CoberturaMatch.from(input.coverage()),
                insights,
                input.risingProducts(),
                input.limitations()
        );
    }

    public static String render(String template, InsightCandidate candidate) {
        if (template == null) {
            return null;
        }
        Matcher matcher = InsightContract.PLACEHOLDER.matcher(template);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value(matcher.group(1), candidate)));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    static String value(String name, InsightCandidate candidate) {
        return switch (name) {
            case InsightContract.SUBJECT -> candidate.subject();
            case InsightContract.CATEGORY -> candidate.categoryName();
            case InsightContract.CURRENT_MONTH -> InsightMonthLabel.of(candidate.bucketKey());
            case InsightContract.COMPARISON_MONTH -> InsightMonthLabel.of(candidate.comparisonBucketKey());
            default -> candidate.metric(name)
                    .map(InsightNarrativeRenderer::format)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Placeholder {{" + name + "}} is not available for candidate " + candidate.id()));
        };
    }

    public static String format(InsightMetric metric) {
        return switch (metric.unit()) {
            case PERCENT -> decimal(Math.abs(metric.value()), 1) + "%";
            case CURRENCY -> NumberFormat.getCurrencyInstance(PT_BR).format(metric.value()).replace(' ', ' ');
            case SCORE -> decimal(metric.value(), 1);
            case COUNT, RANK -> String.valueOf(Math.round(metric.value()));
        };
    }

    private static String decimal(double value, int digits) {
        NumberFormat format = NumberFormat.getNumberInstance(PT_BR);
        format.setMinimumFractionDigits(digits);
        format.setMaximumFractionDigits(digits);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(value);
    }
}
