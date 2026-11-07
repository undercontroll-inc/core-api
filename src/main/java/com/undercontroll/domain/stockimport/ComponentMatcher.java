package com.undercontroll.domain.stockimport;

import com.undercontroll.domain.enums.StockImportMatchType;
import com.undercontroll.domain.gateway.ComponentAliasGateway;
import com.undercontroll.domain.gateway.ComponentGateway;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.stockimport.ComponentMatch;
import com.undercontroll.domain.model.stockimport.MatchCandidate;
import com.undercontroll.domain.support.Texts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ComponentMatcher {

    static final double AUTO_MATCH_SCORE = 0.75;
    static final double MIN_CANDIDATE_SCORE = 0.3;
    static final int MAX_CANDIDATES = 3;
    static final int SEARCH_TOKENS = 3;
    static final int SEARCH_LIMIT = 20;
    static final Set<String> STOPWORDS = Set.of(
            "de", "da", "do", "das", "dos", "para", "com", "sem", "em", "e", "a", "o", "p", "c", "un", "pc", "pcs", "cx");

    private final ComponentGateway componentGateway;
    private final ComponentAliasGateway componentAliasGateway;

    public ComponentMatch match(String supplierKey, String supplierCode, String description) {
        Optional<ComponentPart> aliased = alias(supplierKey, supplierCode, description);
        List<MatchCandidate> candidates = candidates(description);
        if (aliased.isPresent()) {
            ComponentPart component = aliased.get();
            List<MatchCandidate> merged = new ArrayList<>();
            merged.add(candidate(component, 1.0));
            candidates.stream()
                    .filter(candidate -> !candidate.componentId().equals(component.getId()))
                    .limit(MAX_CANDIDATES - 1)
                    .forEach(merged::add);
            return new ComponentMatch(StockImportMatchType.ALIAS, component.getId(), 1.0, merged);
        }
        if (!candidates.isEmpty() && candidates.getFirst().score() >= AUTO_MATCH_SCORE) {
            MatchCandidate best = candidates.getFirst();
            return new ComponentMatch(StockImportMatchType.FUZZY, best.componentId(), best.score(), candidates);
        }
        return ComponentMatch.none(candidates);
    }

    private Optional<ComponentPart> alias(String supplierKey, String supplierCode, String description) {
        if (supplierKey == null) {
            return Optional.empty();
        }
        List<String> productKeys = new ArrayList<>();
        StockImportKeys.productKey(supplierCode, description).ifPresent(productKeys::add);
        StockImportKeys.productKey(null, description).filter(key -> !productKeys.contains(key)).ifPresent(productKeys::add);
        for (String productKey : productKeys) {
            Optional<ComponentPart> component = componentAliasGateway.find(supplierKey, productKey)
                    .flatMap(componentGateway::findById);
            if (component.isPresent()) {
                return component;
            }
        }
        return Optional.empty();
    }

    List<MatchCandidate> candidates(String description) {
        List<String> tokens = tokens(description);
        if (tokens.isEmpty()) {
            return List.of();
        }
        Map<Integer, ComponentPart> pool = new LinkedHashMap<>();
        tokens.stream()
                .filter(token -> token.length() >= 3)
                .sorted(Comparator.comparingInt(String::length).reversed())
                .limit(SEARCH_TOKENS)
                .forEach(token -> componentGateway.searchByName(token, SEARCH_LIMIT)
                        .forEach(component -> pool.putIfAbsent(component.getId(), component)));
        return pool.values().stream()
                .map(component -> candidate(component, score(tokens, componentTokens(component))))
                .filter(candidate -> candidate.score() >= MIN_CANDIDATE_SCORE)
                .sorted(Comparator.comparingDouble(MatchCandidate::score).reversed()
                        .thenComparing(MatchCandidate::componentId))
                .limit(MAX_CANDIDATES)
                .toList();
    }

    static double score(List<String> left, List<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0.0;
        }
        double matchedLeft = left.stream().mapToDouble(token -> best(token, right)).sum();
        double matchedRight = right.stream().mapToDouble(token -> best(token, left)).sum();
        double score = (matchedLeft + matchedRight) / (left.size() + right.size());
        return Math.round(score * 100.0) / 100.0;
    }

    private static double best(String token, List<String> others) {
        double best = 0.0;
        for (String other : others) {
            best = Math.max(best, similarity(token, other));
            if (best == 1.0) {
                break;
            }
        }
        return best;
    }

    private static double similarity(String a, String b) {
        if (a.equals(b)) {
            return 1.0;
        }
        String shorter = a.length() <= b.length() ? a : b;
        String longer = shorter == a ? b : a;
        if (shorter.length() >= 4 && longer.startsWith(shorter)) {
            return 0.8;
        }
        return 0.0;
    }

    static List<String> tokens(String value) {
        String normalized = Texts.normalize(value);
        if (normalized.isEmpty()) {
            return List.of();
        }
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : normalized.split(" ")) {
            if (!token.isEmpty() && !STOPWORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return List.copyOf(tokens);
    }

    private static List<String> componentTokens(ComponentPart component) {
        List<String> tokens = new ArrayList<>(tokens(component.getName()));
        tokens(component.getBrand()).stream().filter(token -> !tokens.contains(token)).forEach(tokens::add);
        return tokens;
    }

    private static MatchCandidate candidate(ComponentPart component, double score) {
        return new MatchCandidate(component.getId(), component.getName(), component.getBrand(),
                component.getQuantity(), component.getPrice(), score);
    }
}
