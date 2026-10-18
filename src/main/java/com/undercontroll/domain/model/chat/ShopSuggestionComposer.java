package com.undercontroll.domain.model.chat;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.model.chat.ShopSnapshot.DemandFact;
import com.undercontroll.domain.model.chat.ShopSnapshot.RepairFact;
import com.undercontroll.domain.model.chat.ShopSnapshot.StockFact;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShopSuggestionComposer {

    public static final List<String> GENERIC = List.of(
            "Quais consertos ainda não foram olhados?",
            "Tem peça acabando no estoque?",
            "Algum conserto está esperando peça?",
            "Tem aviso novo no aplicativo?",
            "Quais consertos já podem ser buscados?"
    );

    public static final int OPEN_LIMIT = 5;
    public static final int PICKUP_LIMIT = 3;
    public static final int STOCK_LIMIT = 5;
    public static final int DEMAND_LIMIT = 5;
    public static final long LOW_STOCK_MAX = 5L;

    private static final int MAX_QUESTION_LENGTH = 90;
    private static final int MENTIONED_ORDER_LIMIT = 3;
    private static final int ORDER_DETAIL_LIMIT = 8;
    private static final Pattern NON_LETTERS = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern MENTIONED_ORDER = Pattern.compile("\\bpedidos?\\s+(\\d+)\\b");
    private static final String LOOKED_AT_SUFFIX = " já foi olhado?";
    private static final String PART_PREFIX = "A peça ";
    private static final Set<String> STOPWORDS = Set.of(
            "o", "a", "os", "as", "de", "do", "da", "dos", "das", "no", "na", "nos", "nas",
            "um", "uma", "uns", "umas", "e", "ou", "que", "qual", "quais", "como", "tem",
            "esta", "está", "ja", "já", "ainda", "pouco", "estoque", "peca", "peça", "pecas",
            "peças", "conserto", "consertos", "pedido", "pedidos", "aviso", "avisos",
            "aplicativo", "oficina", "situacao", "situação"
    );

    private ShopSuggestionComposer() {
    }

    public static ShopSnapshot from(
            List<Order> orders,
            List<ComponentPart> parts,
            List<Demand> demands,
            Announcement lastAnnouncement
    ) {
        List<Order> safeOrders = orders == null ? List.of() : orders;
        List<RepairFact> open = safeOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.PENDING
                        || order.getStatus() == OrderStatus.IN_ANALYSIS)
                .sorted(openRepairOrder())
                .limit(OPEN_LIMIT)
                .map(ShopSuggestionComposer::toRepairFact)
                .toList();
        List<RepairFact> pickup = safeOrders.stream()
                .filter(order -> order.getStatus() == OrderStatus.COMPLETED)
                .sorted(pickupOrder())
                .limit(PICKUP_LIMIT)
                .map(ShopSuggestionComposer::toRepairFact)
                .toList();
        List<StockFact> stock = (parts == null ? List.<ComponentPart>of() : parts).stream()
                .filter(part -> part.getQuantity() != null && part.getQuantity() <= LOW_STOCK_MAX)
                .sorted(Comparator.comparing(ComponentPart::getQuantity))
                .limit(STOCK_LIMIT)
                .map(part -> new StockFact(part.getName(), part.getQuantity()))
                .filter(fact -> fact.name() != null && !fact.name().isBlank())
                .toList();
        List<DemandFact> pending = (demands == null ? List.<Demand>of() : demands).stream()
                .map(ShopSuggestionComposer::toDemandFact)
                .filter(Objects::nonNull)
                .limit(DEMAND_LIMIT)
                .toList();
        String announcement = lastAnnouncement == null ? null : blankToNull(lastAnnouncement.getTitle());
        return new ShopSnapshot(open, pickup, stock, pending, announcement);
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
                        && !similarToAny(candidate, blocked)
                        && !similarToAny(candidate, remaining)) {
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
        if (addUnblocked(pool, selected, List.of(), size)
                || addUnblocked(GENERIC, selected, List.of(), size)) {
            return List.copyOf(selected);
        }
        return List.copyOf(selected);
    }

    public static String chatBriefing(ShopSnapshot snapshot) {
        ShopSnapshot safe = snapshot == null ? ShopSnapshot.empty() : snapshot;
        StringBuilder text = new StringBuilder();
        appendSection(text, "Consertos em andamento", safe.openRepairs().stream()
                .map(ShopSuggestionComposer::repairLine)
                .toList());
        appendSection(text, "Consertos prontos para o cliente buscar", safe.readyForPickup().stream()
                .map(ShopSuggestionComposer::repairLine)
                .toList());
        appendSection(text, "Peças com pouco estoque", safe.lowStockParts().stream()
                .map(part -> part.name() + " (" + part.quantity() + ")")
                .toList());
        appendSection(text, "Peças pedidas nos consertos", safe.pendingParts().stream()
                .map(ShopSuggestionComposer::demandLine)
                .toList());
        if (safe.lastAnnouncementTitle() == null) {
            text.append("Aviso mais recente: nenhum.\n");
        } else {
            text.append("Aviso mais recente: ").append(safe.lastAnnouncementTitle()).append(".\n");
        }
        return text.toString().trim();
    }

    public static List<Integer> mentionedOrderIds(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        Matcher matcher = MENTIONED_ORDER.matcher(normalized);
        LinkedHashSet<Integer> ids = new LinkedHashSet<>();
        while (matcher.find() && ids.size() < MENTIONED_ORDER_LIMIT) {
            ids.add(Integer.parseInt(matcher.group(1)));
        }
        return List.copyOf(ids);
    }

    public static String appendOrderDetails(String briefing, List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return briefing == null ? "" : briefing;
        }
        StringBuilder text = new StringBuilder(briefing == null ? "" : briefing);
        if (!text.isEmpty()) {
            text.append("\n\n");
        }
        text.append("Pedido citado:\n");
        for (Order order : orders) {
            if (order != null) {
                text.append("- ").append(orderDetailLine(order)).append('\n');
            }
        }
        return text.toString().trim();
    }

    public static String prompt(ShopSnapshot snapshot, List<String> avoid, int count) {
        StringBuilder text = new StringBuilder();
        text.append("Situação atual da oficina:\n");
        appendSection(text, "Consertos em andamento", snapshot.openRepairs().stream()
                .map(ShopSuggestionComposer::repairLine)
                .toList());
        appendSection(text, "Consertos prontos para o cliente buscar", snapshot.readyForPickup().stream()
                .map(ShopSuggestionComposer::repairLine)
                .toList());
        appendSection(text, "Peças com pouco estoque", snapshot.lowStockParts().stream()
                .map(part -> part.name() + " (" + part.quantity() + ")")
                .toList());
        appendSection(text, "Peças pedidas nos consertos", snapshot.pendingParts().stream()
                .map(ShopSuggestionComposer::demandLine)
                .toList());
        if (snapshot.lastAnnouncementTitle() == null) {
            text.append("Aviso mais recente: nenhum.\n");
        } else {
            text.append("Aviso mais recente: ").append(snapshot.lastAnnouncementTitle()).append(".\n");
        }
        text.append("\nAtalhos de partida (reescreva no máximo a frase, não o fato; um assunto por atalho):\n");
        for (String question : groundedQuestions(snapshot, count)) {
            text.append("- ").append(question).append('\n');
        }
        if (avoid != null && !avoid.isEmpty()) {
            text.append("\nNão repita estas frases nem o mesmo assunto com outras palavras:\n");
            for (String previous : avoid) {
                if (previous != null && !previous.isBlank()) {
                    text.append("- ").append(previous).append('\n');
                }
            }
        }
        return text.toString();
    }

    public static List<String> mix(
            List<String> generated,
            ShopSnapshot snapshot,
            List<String> avoid,
            int count
    ) {
        int size = Math.max(1, count);
        List<String> selected = new ArrayList<>();
        List<String> blocked = avoid == null ? List.of() : avoid;
        if (addGenerated(generated, snapshot, selected, blocked, size)
                || addUnblocked(groundedQuestions(snapshot, size), selected, blocked, size)) {
            return List.copyOf(selected);
        }
        addUnblocked(GENERIC, selected, List.of(), size);
        return List.copyOf(selected);
    }

    private static boolean addGenerated(
            List<String> generated,
            ShopSnapshot snapshot,
            List<String> selected,
            List<String> blocked,
            int size
    ) {
        if (generated == null) {
            return false;
        }
        boolean requireFact = hasFacts(snapshot);
        for (String raw : generated) {
            String question = clean(raw);
            if (acceptable(question, snapshot, requireFact)
                    && addIfUnique(selected, question, blocked, size)) {
                return true;
            }
        }
        return false;
    }

    private static boolean addUnblocked(
            List<String> questions,
            List<String> selected,
            List<String> blocked,
            int size
    ) {
        for (String question : questions) {
            if (addIfUnique(selected, question, blocked, size)) {
                return true;
            }
        }
        return false;
    }

    private static boolean addIfUnique(List<String> selected, String candidate, int size) {
        return addIfUnique(selected, candidate, List.of(), size);
    }

    private static boolean addIfUnique(
            List<String> selected,
            String candidate,
            List<String> blocked,
            int size
    ) {
        if (candidate != null
                && !candidate.isBlank()
                && !similarToAny(candidate, selected)
                && !similarToAny(candidate, blocked)) {
            selected.add(candidate);
        }
        return selected.size() >= size;
    }

    private static List<String> candidates(ShopSnapshot snapshot) {
        List<String> questions = new ArrayList<>();
        List<RepairFact> open = snapshot.openRepairs();
        List<StockFact> stock = snapshot.lowStockParts();
        List<DemandFact> pending = snapshot.pendingParts();
        List<RepairFact> pickup = snapshot.readyForPickup();
        addFirst(questions, open, ShopSuggestionComposer::repairQuestion);
        addFirst(questions, stock, ShopSuggestionComposer::stockQuestion);
        addFirst(questions, pending, ShopSuggestionComposer::demandQuestion);
        addFirst(questions, pickup, ShopSuggestionComposer::pickupQuestion);
        if (snapshot.lastAnnouncementTitle() != null) {
            questions.add("O que diz o aviso " + clip(snapshot.lastAnnouncementTitle(), 32) + "?");
        }
        addRest(questions, open, ShopSuggestionComposer::repairQuestion);
        addRest(questions, stock, ShopSuggestionComposer::stockQuestion);
        addRest(questions, pending, ShopSuggestionComposer::demandQuestion);
        addRest(questions, pickup, ShopSuggestionComposer::pickupQuestion);
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
        boolean pending = "ainda não olhado".equals(repair.statusLabel());
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

    private static RepairFact toRepairFact(Order order) {
        return new RepairFact(
                order.getId(),
                customerName(order.getUser()),
                applianceOf(order),
                statusLabel(order.getStatus())
        );
    }

    private static DemandFact toDemandFact(Demand demand) {
        if (demand == null || demand.getComponent() == null) {
            return null;
        }
        String name = blankToNull(demand.getComponent().getName());
        if (name == null) {
            return null;
        }
        Integer orderId = demand.getOrder() == null ? null : demand.getOrder().getId();
        return new DemandFact(name, orderId, demand.getQuantity());
    }

    private static String customerName(User user) {
        if (user == null) {
            return null;
        }
        String joined = String.join(" ",
                Objects.toString(user.getName(), ""),
                Objects.toString(user.getLastName(), "")).trim();
        return joined.isBlank() ? null : joined;
    }

    private static String applianceOf(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return clip(blankToNull(order.getCustomerDescription()), 28);
        }
        OrderItem item = order.getOrderItems().getFirst();
        String type = blankToNull(item.getType());
        String brand = blankToNull(item.getBrand());
        if (type != null) {
            type = type.toLowerCase(Locale.ROOT);
        }
        if (type != null && brand != null) {
            return clip(type + " " + brand, 28);
        }
        if (type != null) {
            return clip(type, 28);
        }
        if (brand != null) {
            return clip(brand, 28);
        }
        return clip(blankToNull(item.getModel()), 28);
    }

    private static String statusLabel(OrderStatus status) {
        if (status == null) {
            return "em andamento";
        }
        return switch (status) {
            case PENDING -> "ainda não olhado";
            case IN_ANALYSIS -> "em análise";
            case COMPLETED -> "pronto para buscar";
            case DELIVERED -> "entregue";
        };
    }

    private static String repairLine(RepairFact repair) {
        List<String> bits = new ArrayList<>();
        if (repair.orderId() != null) {
            bits.add("pedido " + repair.orderId());
        }
        if (repair.customer() != null) {
            bits.add("cliente " + repair.customer());
        }
        if (repair.appliance() != null) {
            bits.add(repair.appliance());
        }
        bits.add(repair.statusLabel());
        return String.join(", ", bits);
    }

    private static String orderDetailLine(Order order) {
        String header = repairLine(toRepairFact(order));
        List<String> extras = new ArrayList<>();
        if (order.getOrderItems() != null) {
            order.getOrderItems().stream()
                    .limit(ORDER_DETAIL_LIMIT)
                    .map(item -> String.join(" ",
                            Objects.toString(blankToNull(item.getType()), ""),
                            Objects.toString(blankToNull(item.getBrand()), "")).trim())
                    .filter(part -> !part.isBlank())
                    .forEach(extras::add);
        }
        if (order.getDemands() != null) {
            order.getDemands().stream()
                    .limit(ORDER_DETAIL_LIMIT)
                    .map(ShopSuggestionComposer::toDemandFact)
                    .filter(Objects::nonNull)
                    .map(fact -> fact.partName() + " qtd " + fact.quantity())
                    .forEach(extras::add);
        }
        if (extras.isEmpty()) {
            return header;
        }
        return header + "; " + String.join("; ", extras);
    }

    private static String demandLine(DemandFact demand) {
        String line = demand.partName();
        if (demand.orderId() != null) {
            line += ", pedido " + demand.orderId();
        }
        if (demand.quantity() != null) {
            line += ", qtd " + demand.quantity();
        }
        return line;
    }

    private static void appendSection(StringBuilder text, String title, List<String> lines) {
        text.append(title).append(":\n");
        if (lines.isEmpty()) {
            text.append("- nenhum\n");
            return;
        }
        for (String line : lines) {
            text.append("- ").append(line).append('\n');
        }
    }

    private static Comparator<Order> openRepairOrder() {
        return Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(Order::getId, Comparator.nullsLast(Integer::compareTo));
    }

    private static Comparator<Order> pickupOrder() {
        return Comparator.comparing(Order::getCompletedTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Order::getId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private static boolean hasFacts(ShopSnapshot snapshot) {
        return !snapshot.openRepairs().isEmpty()
                || !snapshot.readyForPickup().isEmpty()
                || !snapshot.lowStockParts().isEmpty()
                || !snapshot.pendingParts().isEmpty()
                || snapshot.lastAnnouncementTitle() != null;
    }

    private static boolean acceptable(String question, ShopSnapshot snapshot, boolean requireFact) {
        if (question == null || question.isBlank() || question.length() > MAX_QUESTION_LENGTH) {
            return false;
        }
        if (question.contains("{") || question.contains("}")) {
            return false;
        }
        return !requireFact || mentionsFact(question, snapshot);
    }

    private static boolean mentionsFact(String question, ShopSnapshot snapshot) {
        String normalized = normalize(question);
        for (String token : factTokens(snapshot)) {
            if (normalized.contains(normalize(token))) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> factTokens(ShopSnapshot snapshot) {
        Set<String> tokens = new LinkedHashSet<>();
        for (RepairFact repair : snapshot.openRepairs()) {
            addFactToken(tokens, firstName(repair.customer()));
            addFactToken(tokens, repair.appliance());
            if (repair.orderId() != null) {
                tokens.add(String.valueOf(repair.orderId()));
            }
        }
        for (RepairFact repair : snapshot.readyForPickup()) {
            addFactToken(tokens, firstName(repair.customer()));
            addFactToken(tokens, repair.appliance());
            if (repair.orderId() != null) {
                tokens.add(String.valueOf(repair.orderId()));
            }
        }
        for (StockFact part : snapshot.lowStockParts()) {
            addFactToken(tokens, part.name());
        }
        for (DemandFact demand : snapshot.pendingParts()) {
            addFactToken(tokens, demand.partName());
            if (demand.orderId() != null) {
                tokens.add(String.valueOf(demand.orderId()));
            }
        }
        addFactToken(tokens, snapshot.lastAnnouncementTitle());
        return tokens;
    }

    private static void addFactToken(Set<String> tokens, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        for (String part : value.toLowerCase(Locale.ROOT).split("\\s+")) {
            String cleaned = NON_LETTERS.matcher(stripAccents(part)).replaceAll("");
            if (cleaned.length() >= 3 || cleaned.chars().allMatch(Character::isDigit)) {
                tokens.add(cleaned);
            }
        }
    }

    private static boolean similarToAny(String question, List<String> existing) {
        for (String other : existing) {
            if (similar(question, other)) {
                return true;
            }
        }
        return false;
    }

    static boolean similar(String left, String right) {
        String a = normalize(left);
        String b = normalize(right);
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

    private static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.strip().replaceAll("^[\"']", "").replaceAll("[\"']$", "");
        if (trimmed.endsWith("..")) {
            return trimmed;
        }
        return trimmed;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return NON_LETTERS.matcher(stripAccents(value.toLowerCase(Locale.ROOT))).replaceAll(" ").strip()
                .replaceAll("\\s+", " ");
    }

    private static String stripAccents(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    private static String firstName(String customer) {
        if (customer == null || customer.isBlank()) {
            return null;
        }
        return customer.strip().split("\\s+")[0];
    }

    private static String clip(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max).strip();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
