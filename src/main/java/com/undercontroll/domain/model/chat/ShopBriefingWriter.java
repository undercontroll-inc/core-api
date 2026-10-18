package com.undercontroll.domain.model.chat;

import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.chat.ShopSnapshot.DemandFact;
import com.undercontroll.domain.model.chat.ShopSnapshot.RepairFact;
import com.undercontroll.domain.support.Texts;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShopBriefingWriter {

    private static final int MENTIONED_ORDER_LIMIT = 3;
    private static final int ORDER_DETAIL_LIMIT = 8;
    private static final Pattern MENTIONED_ORDER = Pattern.compile("\\bpedidos?\\s+(\\d+)\\b");

    private ShopBriefingWriter() {
    }

    public static String chatBriefing(ShopSnapshot snapshot) {
        ShopSnapshot safe = snapshot == null ? ShopSnapshot.empty() : snapshot;
        StringBuilder text = new StringBuilder();
        appendSection(text, "Consertos em andamento", safe.openRepairs().stream()
                .map(ShopBriefingWriter::repairLine)
                .toList());
        appendSection(text, "Consertos prontos para o cliente buscar", safe.readyForPickup().stream()
                .map(ShopBriefingWriter::repairLine)
                .toList());
        appendSection(text, "Peças com pouco estoque", safe.lowStockParts().stream()
                .map(part -> part.name() + " (" + part.quantity() + ")")
                .toList());
        appendSection(text, "Peças pedidas nos consertos", safe.pendingParts().stream()
                .map(ShopBriefingWriter::demandLine)
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
        String normalized = Texts.stripAccents(text.toLowerCase(Locale.ROOT));
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
        String header = repairLine(ShopSnapshotFactory.toRepairFact(order));
        List<String> extras = new ArrayList<>();
        if (order.getOrderItems() != null) {
            order.getOrderItems().stream()
                    .limit(ORDER_DETAIL_LIMIT)
                    .map(item -> String.join(" ",
                            Objects.toString(Texts.blankToNull(item.getType()), ""),
                            Objects.toString(Texts.blankToNull(item.getBrand()), "")).trim())
                    .filter(part -> !part.isBlank())
                    .forEach(extras::add);
        }
        if (order.getDemands() != null) {
            order.getDemands().stream()
                    .limit(ORDER_DETAIL_LIMIT)
                    .map(ShopSnapshotFactory::toDemandFact)
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
}
