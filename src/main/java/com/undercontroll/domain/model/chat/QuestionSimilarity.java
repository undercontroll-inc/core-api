package com.undercontroll.domain.model.chat;

import com.undercontroll.domain.support.Texts;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class QuestionSimilarity {

    private static final Set<String> STOPWORDS = Set.of(
            "o", "a", "os", "as", "de", "do", "da", "dos", "das", "no", "na", "nos", "nas",
            "um", "uma", "uns", "umas", "e", "ou", "que", "qual", "quais", "como", "tem",
            "esta", "está", "ja", "já", "ainda", "pouco", "estoque", "peca", "peça", "pecas",
            "peças", "conserto", "consertos", "pedido", "pedidos", "aviso", "avisos",
            "aplicativo", "oficina", "situacao", "situação"
    );

    private QuestionSimilarity() {
    }

    public static boolean similarToAny(String question, List<String> existing) {
        for (String other : existing) {
            if (similar(question, other)) {
                return true;
            }
        }
        return false;
    }

    public static boolean similar(String left, String right) {
        String a = Texts.normalize(left);
        String b = Texts.normalize(right);
        if (a.equals(b) || a.contains(b) || b.contains(a)) {
            return true;
        }
        Set<String> leftTokens = significantTokens(a);
        Set<String> rightTokens = significantTokens(b);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return false;
        }
        Set<String> shared = new LinkedHashSet<>(leftTokens);
        shared.retainAll(rightTokens);
        if (shared.isEmpty()) {
            return false;
        }
        int union = leftTokens.size() + rightTokens.size() - shared.size();
        return shared.size() >= 2 && (double) shared.size() / union >= 0.5;
    }

    private static Set<String> significantTokens(String normalized) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : normalized.split("\\s+")) {
            if (token.isBlank() || STOPWORDS.contains(token)) {
                continue;
            }
            if (token.length() >= 4 || token.chars().allMatch(Character::isDigit)) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
