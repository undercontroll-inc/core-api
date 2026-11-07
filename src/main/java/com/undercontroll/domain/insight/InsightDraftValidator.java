package com.undercontroll.domain.insight;

import com.undercontroll.domain.model.insight.InsightCandidate;
import com.undercontroll.domain.model.insight.InsightDraft;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class InsightDraftValidator {

    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SALES_LANGUAGE = Pattern.compile(
            "vendeu|vendas? em unidades|unidades vendidas|l[ií]der de vendas|volume de vendas|faturamento",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );
    private static final Pattern JARGON = Pattern.compile(
            "\\bbuckets?\\b|domain_id|vw_market_|candidate_id|\\bscore\\b|\\brank\\b|\\bMLB[-_]",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private InsightDraftValidator() {
    }

    public static List<String> validate(InsightDraft draft, List<InsightCandidate> candidates, InsightContract contract) {
        List<String> errors = new ArrayList<>();
        if (draft == null) {
            errors.add("draft is empty");
            return errors;
        }
        if (draft.insights().size() > contract.maxInsights()) {
            errors.add("insights has " + draft.insights().size() + " items; the maximum is " + contract.maxInsights());
        }
        Map<String, InsightCandidate> byId = candidates.stream()
                .collect(Collectors.toMap(InsightCandidate::id, Function.identity(), (first, second) -> first));
        Set<String> used = new HashSet<>();
        for (int index = 0; index < draft.insights().size(); index++) {
            InsightDraft.Item item = draft.insights().get(index);
            String path = "insights[" + index + "]";
            if (item == null) {
                errors.add(path + " is empty");
                continue;
            }
            InsightCandidate candidate = byId.get(item.candidateId());
            if (candidate == null) {
                errors.add(path + ".candidate_id " + item.candidateId() + " is not one of the candidates");
                continue;
            }
            if (!used.add(candidate.id())) {
                errors.add(path + ".candidate_id " + candidate.id() + " is repeated");
            }
            validateText(errors, path + ".text", item.text(), candidate, InsightContract.MIN_TEXT_LENGTH,
                    InsightContract.MAX_TEXT_LENGTH);
            validateText(errors, path + ".action", item.action(), candidate, 1, InsightContract.MAX_ACTION_LENGTH);
        }
        return errors;
    }

    private static void validateText(
            List<String> errors,
            String path,
            String text,
            InsightCandidate candidate,
            int minLength,
            int maxLength
    ) {
        if (text == null || text.isBlank()) {
            errors.add(path + " is required");
            return;
        }
        Set<String> allowed = InsightContract.placeholders(candidate);
        Matcher matcher = InsightContract.PLACEHOLDER.matcher(text);
        boolean placeholdersValid = true;
        while (matcher.find()) {
            if (!allowed.contains(matcher.group(1))) {
                errors.add(path + " uses {{" + matcher.group(1) + "}}, which candidate " + candidate.id()
                        + " does not have; allowed: " + String.join(", ", allowed));
                placeholdersValid = false;
            }
        }
        String literal = InsightContract.PLACEHOLDER.matcher(text).replaceAll("");
        if (DIGIT.matcher(literal).find()) {
            errors.add(path + " writes digits directly; every number, month or product name must be a placeholder");
        }
        if (SALES_LANGUAGE.matcher(literal).find()) {
            errors.add(path + " claims sales volume; the data measures popularity and listed prices");
        }
        if (JARGON.matcher(literal).find()) {
            errors.add(path + " uses internal jargon (bucket, rank, score, ids or view names)");
        }
        if (placeholdersValid) {
            int length = InsightNarrativeRenderer.render(text, candidate).length();
            if (length < minLength || length > maxLength) {
                errors.add(path + " has " + length + " characters after filling placeholders; expected "
                        + minLength + " to " + maxLength);
            }
        }
    }
}
