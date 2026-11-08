package com.undercontroll.domain.order;

import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.model.User;

import java.util.List;
import java.util.Locale;

public final class OrderMessages {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private OrderMessages() {
    }

    public static String whatsapp(Order order, String trackingUrl) {
        String greeting = greeting(order.getUser());
        String device = device(order.getOrderItems());
        String body;
        if (order.getStatus() == OrderStatus.AWAITING_APPROVAL) {
            body = "O orçamento do conserto " + of(device) + " ficou em " + money(order.getBudgetTotal())
                    + ". Para aprovar ou recusar, toque no link: ";
        } else if (order.getStatus() == OrderStatus.READY_FOR_PICKUP && order.getOutcome() == OrderOutcome.NOT_REPAIRED) {
            body = capitalize(device) + " já pode ser retirado na loja, sem custo. Detalhes: ";
        } else if (order.getStatus() == OrderStatus.READY_FOR_PICKUP) {
            body = capitalize(device) + " está pronto. Pode buscar na loja. Detalhes: ";
        } else {
            body = "Acompanhe o conserto " + of(device) + " por aqui: ";
        }
        return greeting + " " + body + trackingUrl;
    }

    public static String firstName(User user) {
        if (user == null || user.getName() == null || user.getName().isBlank()) {
            return null;
        }
        String first = user.getName().strip().split("\\s+")[0];
        return first.substring(0, 1).toUpperCase(PT_BR) + first.substring(1);
    }

    public static String device(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return "seu aparelho";
        }
        if (items.size() > 1) {
            return "seus aparelhos";
        }
        OrderItem item = items.getFirst();
        String type = item.getType() == null || item.getType().isBlank() ? "aparelho" : item.getType().strip().toLowerCase(PT_BR);
        String brand = item.getBrand() == null || item.getBrand().isBlank() ? "" : " " + item.getBrand().strip();
        return "seu " + type + brand;
    }

    public static String money(Double value) {
        return String.format(PT_BR, "R$ %,.2f", value == null ? 0.0 : value);
    }

    private static String greeting(User user) {
        String name = firstName(user);
        return name == null ? "Olá!" : "Olá, " + name + "!";
    }

    private static String of(String device) {
        return device.startsWith("seus ") ? "dos " + device : "do " + device;
    }

    private static String capitalize(String value) {
        return value.substring(0, 1).toUpperCase(PT_BR) + value.substring(1);
    }
}
