package com.undercontroll.domain.model.chat;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopSuggestionsTest {

    @Test
    @DisplayName("builds one useful question per shop fact instead of repeating open repairs")
    void groundedQuestionsCoverDistinctFacts() {
        ShopSnapshot snapshot = sampleShop();

        List<String> questions = GroundedQuestionGenerator.groundedQuestions(snapshot, 4);

        assertEquals(4, questions.size());
        String joined = String.join(" | ", questions);
        assertTrue(joined.contains("Maria"));
        assertTrue(joined.contains("Resistência"));
        assertTrue(joined.contains("Capacitor"));
        assertTrue(joined.contains("João"));
        assertFalse(QuestionSimilarity.similar(questions.get(0), questions.get(1)));
        assertFalse(QuestionSimilarity.similar(questions.get(0), questions.get(2)));
    }

    @Test
    @DisplayName("refresh avoids the previous lot and picks other shop facts")
    void groundedQuestionsAvoidPreviousLot() {
        ShopSnapshot snapshot = richShop();
        List<String> first = GroundedQuestionGenerator.groundedQuestions(snapshot, 4);

        List<String> next = GroundedQuestionGenerator.groundedQuestions(snapshot, first, 4);

        assertEquals(4, next.size());
        String joined = String.join(" | ", next);
        assertTrue(joined.contains("Ana")
                || joined.contains("Compressor")
                || joined.contains("Motor")
                || joined.contains("Pedro")
                || joined.contains("Feriado"));
        for (String previous : first) {
            for (String candidate : next) {
                assertFalse(QuestionSimilarity.similar(previous, candidate),
                        () -> previous + " ~ " + candidate);
            }
        }
    }

    @Test
    @DisplayName("chat briefing lists open and pickup repairs, not delivered ones")
    void chatBriefing() {
        String briefing = ShopBriefingWriter.chatBriefing(sampleShop());

        assertTrue(briefing.contains("Maria"));
        assertTrue(briefing.contains("liquidificador"));
        assertTrue(briefing.contains("em andamento"));
        assertTrue(briefing.contains("João"));
        assertTrue(briefing.contains("pronto para buscar"));
        assertTrue(briefing.contains("Resistência"));
        assertTrue(briefing.contains("Capacitor"));
        assertTrue(briefing.contains("Feriado"));
        assertFalse(briefing.contains("entregue"));
    }

    @Test
    @DisplayName("chat briefing states the real totals, flags truncated lists and the snapshot time")
    void chatBriefingTotals() {
        ShopSnapshot base = sampleShop();
        ShopSnapshot snapshot = new ShopSnapshot(
                base.openRepairs(), base.readyForPickup(), base.lowStockParts(), base.pendingParts(),
                base.lastAnnouncementTitle(), 12, base.readyForPickup().size(),
                java.time.LocalDateTime.of(2026, 10, 17, 9, 30));

        String briefing = ShopBriefingWriter.chatBriefing(snapshot);

        assertTrue(snapshot.moreOpen());
        assertFalse(snapshot.morePickup());
        assertTrue(briefing.contains("Retrato da oficina de 17/10/2026 09:30 (UTC)"));
        assertTrue(briefing.contains("Consertos em andamento (mostrando " + base.openRepairs().size() + " de 12; há mais, use list_orders"));
        assertTrue(briefing.contains("Consertos prontos para o cliente buscar (total " + base.readyForPickup().size() + ")"));
    }

    @Test
    @DisplayName("factory totals never undercount what is shown")
    void factoryTotals() {
        ShopSnapshot snapshot = ShopSnapshotFactory.from(
                List.of(Order.builder().id(1).status(OrderStatus.PENDING).build()),
                List.of(), List.of(), null, 0, 7, null);

        assertEquals(1, snapshot.openTotal());
        assertEquals(7, snapshot.pickupTotal());
    }

    @Test
    @DisplayName("extracts pedido ids from shop questions")
    void mentionedOrderIds() {
        assertEquals(List.of(12), ShopBriefingWriter.mentionedOrderIds("Me fala do pedido 12"));
        assertEquals(List.of(3, 4), ShopBriefingWriter.mentionedOrderIds("pedidos 3 e pedido 4"));
        assertTrue(ShopBriefingWriter.mentionedOrderIds("qual o pedido da pizza").isEmpty());
    }

    @Test
    @DisplayName("appends cited order items and parts to the briefing")
    void appendOrderDetails() {
        Order order = Order.builder()
                .id(12)
                .status(OrderStatus.PENDING)
                .user(User.builder().name("Maria").lastName("Souza").build())
                .orderItems(List.of(OrderItem.builder().type("liquidificador").brand("Mondial").build()))
                .demands(List.of(Demand.builder()
                        .quantity(2L)
                        .component(ComponentPart.builder().name("Resistência").build())
                        .build()))
                .build();

        String briefing = ShopBriefingWriter.appendOrderDetails("resumo", List.of(order));

        assertTrue(briefing.contains("resumo"));
        assertTrue(briefing.contains("pedido 12"));
        assertTrue(briefing.contains("Maria"));
        assertTrue(briefing.contains("liquidificador"));
        assertTrue(briefing.contains("Resistência"));
    }

    private static ShopSnapshot richShop() {
        return ShopSnapshotFactory.from(
                List.of(
                        Order.builder()
                                .id(12)
                                .status(OrderStatus.PENDING)
                                .user(User.builder().name("Maria").lastName("Souza").build())
                                .orderItems(List.of(
                                        OrderItem.builder().type("liquidificador").brand("Mondial").build()
                                ))
                                .build(),
                        Order.builder()
                                .id(15)
                                .status(OrderStatus.IN_ANALYSIS)
                                .user(User.builder().name("Ana").lastName("Costa").build())
                                .orderItems(List.of(
                                        OrderItem.builder().type("geladeira").brand("Brastemp").build()
                                ))
                                .build(),
                        Order.builder()
                                .id(20)
                                .status(OrderStatus.COMPLETED)
                                .user(User.builder().name("João").lastName("Lima").build())
                                .orderItems(List.of(
                                        OrderItem.builder().type("airfryer").brand("Philco").build()
                                ))
                                .build(),
                        Order.builder()
                                .id(21)
                                .status(OrderStatus.COMPLETED)
                                .user(User.builder().name("Pedro").lastName("Nunes").build())
                                .orderItems(List.of(
                                        OrderItem.builder().type("microondas").brand("Electrolux").build()
                                ))
                                .build()
                ),
                List.of(
                        ComponentPart.builder().name("Resistência").quantity(2L).build(),
                        ComponentPart.builder().name("Compressor").quantity(1L).build()
                ),
                List.of(
                        Demand.builder()
                                .quantity(1L)
                                .component(ComponentPart.builder().name("Capacitor").build())
                                .order(Order.builder().id(8).build())
                                .build(),
                        Demand.builder()
                                .quantity(2L)
                                .component(ComponentPart.builder().name("Motor").build())
                                .order(Order.builder().id(9).build())
                                .build()
                ),
                Announcement.builder().title("Feriado").build()
        );
    }

    private static ShopSnapshot sampleShop() {
        return ShopSnapshotFactory.from(
                List.of(
                        Order.builder()
                                .id(12)
                                .status(OrderStatus.PENDING)
                                .user(User.builder().name("Maria").lastName("Souza").build())
                                .orderItems(List.of(
                                        OrderItem.builder().type("liquidificador").brand("Mondial").build()
                                ))
                                .build(),
                        Order.builder()
                                .id(20)
                                .status(OrderStatus.COMPLETED)
                                .user(User.builder().name("João").lastName("Lima").build())
                                .orderItems(List.of(
                                        OrderItem.builder().type("airfryer").brand("Philco").build()
                                ))
                                .build()
                ),
                List.of(ComponentPart.builder().name("Resistência").quantity(2L).build()),
                List.of(Demand.builder()
                        .quantity(1L)
                        .component(ComponentPart.builder().name("Capacitor").build())
                        .order(Order.builder().id(8).build())
                        .build()),
                Announcement.builder().title("Feriado").build()
        );
    }
}
