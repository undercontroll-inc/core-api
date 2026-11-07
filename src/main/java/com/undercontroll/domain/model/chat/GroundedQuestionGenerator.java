package com.undercontroll.domain.model.chat;

import com.undercontroll.domain.model.chat.ShopSnapshot.DemandFact;
import com.undercontroll.domain.model.chat.ShopSnapshot.RepairFact;
import com.undercontroll.domain.model.chat.ShopSnapshot.StockFact;
import com.undercontroll.domain.support.Texts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public final class GroundedQuestionGenerator {

    public static final List<String> GENERIC = List.of(
            "Quais consertos ainda não foram olhados?",
            "Tem peça acabando no estoque?",
            "Algum conserto está esperando peça?",
            "Tem aviso novo no aplicativo?",
            "Quais consertos já podem ser buscados?"
    );

    private static final int ANNOUNCEMENT_MAX_LENGTH = 32;
    private static final String LOOKED_AT_SUFFIX = " já foi olhado?";
    private static final String PART_PREFIX = "A peça ";

    private GroundedQuestionGenerator() {
    }

    public static List<String> groundedQuestions(ShopSnapshot snapshot, int count) {
        return groundedQuestions(snapshot, List.of(), count);
    }

    public static List<String> groundedQuestions(ShopSnapshot snapshot, List<String> avoid, int count) {
        int size = Math.max(1, count);
        ShopSnapshot safe = snapshot == null ? ShopSnapshot.empty() : snapshot;
        List<String> blocked = avoid == null ? List.of() : avoid;
        List<String> pool = candidates(safe);
        List<String> selected = new ArrayList<>();
        if (blocked.isEmpty()) {
            if (addUnblocked(pool, selected, List.of(), size)) {
                return List.copyOf(selected);
            }
        } else {
            List<String> remaining = new ArrayList<>();
            for (String candidate : pool) {
                if (candidate != null
                        && !candidate.isBlank()
                        && !QuestionSimilarity.similarToAny(candidate, blocked)
                        && !QuestionSimilarity.similarToAny(candidate, remaining)) {
                    remaining.add(candidate);
                }
            }
            Collections.shuffle(remaining);
            if (addUnblocked(remaining, selected, blocked, size)) {
                return List.copyOf(selected);
            }
        }
        if (addUnblocked(GENERIC, selected, blocked, size)) {
            return List.copyOf(selected);
        }
        if (!addUnblocked(pool, selected, List.of(), size)) {
            addUnblocked(GENERIC, selected, List.of(), size);
        }
        return List.copyOf(selected);
    }

    private static boolean addUnblocked(
            List<String> questions,
            List<String> selected,
            List<String> blocked,
            int size
    ) {
        for (String question : questions) {
            if (question != null
                    && !question.isBlank()
                    && !QuestionSimilarity.similarToAny(question, selected)
                    && !QuestionSimilarity.similarToAny(question, blocked)) {
                selected.add(question);
            }
            if (selected.size() >= size) {
                return true;
            }
        }
        return false;
    }

    private static List<String> candidates(ShopSnapshot snapshot) {
        List<String> questions = new ArrayList<>();
        List<RepairFact> open = snapshot.openRepairs();
        List<StockFact> stock = snapshot.lowStockParts();
        List<DemandFact> pending = snapshot.pendingParts();
        List<RepairFact> pickup = snapshot.readyForPickup();
        addFirst(questions, open, GroundedQuestionGenerator::repairQuestion);
        addFirst(questions, stock, GroundedQuestionGenerator::stockQuestion);
        addFirst(questions, pending, GroundedQuestionGenerator::demandQuestion);
        addFirst(questions, pickup, GroundedQuestionGenerator::pickupQuestion);
        if (snapshot.lastAnnouncementTitle() != null) {
            questions.add("O que diz o aviso " + Texts.clip(snapshot.lastAnnouncementTitle(), ANNOUNCEMENT_MAX_LENGTH) + "?");
        }
        addRest(questions, open, GroundedQuestionGenerator::repairQuestion);
        addRest(questions, stock, GroundedQuestionGenerator::stockQuestion);
        addRest(questions, pending, GroundedQuestionGenerator::demandQuestion);
        addRest(questions, pickup, GroundedQuestionGenerator::pickupQuestion);
        return questions;
    }

    private static <T> void addFirst(List<String> questions, List<T> facts, Function<T, String> toQuestion) {
        if (!facts.isEmpty()) {
            String question = toQuestion.apply(facts.getFirst());
            if (question != null) {
                questions.add(question);
            }
        }
    }

    private static <T> void addRest(List<String> questions, List<T> facts, Function<T, String> toQuestion) {
        for (int i = 1; i < facts.size(); i++) {
            String question = toQuestion.apply(facts.get(i));
            if (question != null) {
                questions.add(question);
            }
        }
    }

    private static String repairQuestion(RepairFact repair) {
        String who = firstName(repair.customer());
        String appliance = repair.appliance();
        boolean pending = repair.notLookedAt();
        if (who != null && appliance != null) {
            if (pending) {
                return "O " + appliance + " de " + who + LOOKED_AT_SUFFIX;
            }
            return "Como está o conserto do " + appliance + " de " + who + "?";
        }
        if (who != null) {
            return pending
                    ? "O conserto de " + who + LOOKED_AT_SUFFIX
                    : "Como está o conserto de " + who + "?";
        }
        if (appliance != null) {
            return pending
                    ? "O " + appliance + LOOKED_AT_SUFFIX
                    : "Como está o conserto do " + appliance + "?";
        }
        if (repair.orderId() != null) {
            return "Qual a situação do pedido " + repair.orderId() + "?";
        }
        return null;
    }

    private static String stockQuestion(StockFact part) {
        if (part.quantity() <= 0) {
            return PART_PREFIX + part.name() + " acabou no estoque?";
        }
        if (part.quantity() <= 2) {
            return PART_PREFIX + part.name() + " está acabando?";
        }
        return "Ainda tem " + part.name() + " no estoque?";
    }

    private static String demandQuestion(DemandFact demand) {
        if (demand.orderId() != null) {
            return PART_PREFIX + demand.partName() + " do pedido " + demand.orderId() + " já chegou?";
        }
        return PART_PREFIX + demand.partName() + " pedida já chegou?";
    }

    private static String pickupQuestion(RepairFact repair) {
        String who = firstName(repair.customer());
        String appliance = repair.appliance();
        if (who != null && appliance != null) {
            return who + " já pode buscar o " + appliance + "?";
        }
        if (who != null) {
            return who + " já pode buscar o conserto?";
        }
        if (repair.orderId() != null) {
            return "O pedido " + repair.orderId() + " já pode ser buscado?";
        }
        return "Tem conserto pronto para o cliente buscar?";
    }

    private static String firstName(String customer) {
        if (customer == null || customer.isBlank()) {
            return null;
        }
        return customer.strip().split("\\s+")[0];
    }
}
