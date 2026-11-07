package com.undercontroll.domain.model.chat;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.model.chat.ShopSnapshot.DemandFact;
import com.undercontroll.domain.model.chat.ShopSnapshot.RepairFact;
import com.undercontroll.domain.model.chat.ShopSnapshot.StockFact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroundedQuestionGeneratorTest {

    private static ShopSnapshot snapshot(List<RepairFact> open, List<RepairFact> pickup, List<StockFact> stock, List<DemandFact> demands) {
        return new ShopSnapshot(open, pickup, stock, demands, null);
    }

    @Test
    @DisplayName("repair questions adapt to the facts that are known")
    void repairQuestionVariants() {
        ShopSnapshot shop = snapshot(List.of(
                new RepairFact(1, "Maria Souza", null, RepairFact.NOT_LOOKED_AT),
                new RepairFact(2, null, "fogão Brastemp", RepairFact.NOT_LOOKED_AT),
                new RepairFact(3, "Ana", null, "em análise"),
                new RepairFact(4, null, "geladeira", "em análise"),
                new RepairFact(5, null, null, "em análise")
        ), List.of(), List.of(), List.of());

        List<String> questions = GroundedQuestionGenerator.groundedQuestions(shop, 5);

        assertTrue(questions.contains("O conserto de Maria já foi olhado?"));
        assertTrue(questions.contains("O fogão Brastemp já foi olhado?"));
        assertTrue(questions.contains("Como está o conserto de Ana?"));
        assertTrue(questions.contains("Como está o conserto do geladeira?"));
        assertTrue(questions.contains("Qual a situação do pedido 5?"));
    }

    @Test
    @DisplayName("stock, demand and pickup questions cover every quantity and identity case")
    void otherQuestionVariants() {
        ShopSnapshot shop = snapshot(
                List.of(),
                List.of(
                        new RepairFact(7, "João Lima", null, "pronto para buscar"),
                        new RepairFact(8, null, null, "pronto para buscar"),
                        new RepairFact(null, null, null, "pronto para buscar")
                ),
                List.of(new StockFact("Fusível", 0), new StockFact("Motor", 4)),
                List.of(new DemandFact("Correia", null, 1L))
        );

        List<String> questions = GroundedQuestionGenerator.groundedQuestions(shop, 7);

        assertTrue(questions.contains("A peça Fusível acabou no estoque?"));
        assertTrue(questions.contains("Ainda tem Motor no estoque?"));
        assertTrue(questions.contains("A peça Correia pedida já chegou?"));
        assertTrue(questions.contains("João já pode buscar o conserto?"));
        assertTrue(questions.contains("O pedido 8 já pode ser buscado?"));
        assertTrue(questions.contains("Tem conserto pronto para o cliente buscar?"));
    }

    @Test
    @DisplayName("an unknown repair yields no question and the lot is filled with generic ones")
    void fillsWithGenericQuestions() {
        ShopSnapshot shop = snapshot(List.of(new RepairFact(null, null, null, "em análise")), List.of(), List.of(), List.of());

        List<String> questions = GroundedQuestionGenerator.groundedQuestions(shop, 3);

        assertEquals(GroundedQuestionGenerator.GENERIC.subList(0, 3), questions);
        assertEquals(1, GroundedQuestionGenerator.groundedQuestions(null, 0).size());
    }

    @Test
    @DisplayName("when everything was already shown it falls back to repeating facts and generic questions")
    void fallsBackWhenEverythingWasShown() {
        ShopSnapshot shop = snapshot(List.of(), List.of(), List.of(new StockFact("Fusível", 1)), List.of());
        List<String> shown = new java.util.ArrayList<>(GroundedQuestionGenerator.GENERIC);
        shown.add("A peça Fusível está acabando?");

        List<String> questions = GroundedQuestionGenerator.groundedQuestions(shop, shown, 3);

        assertEquals(3, questions.size());
        assertEquals("A peça Fusível está acabando?", questions.getFirst());
        assertTrue(GroundedQuestionGenerator.groundedQuestions(shop, null, 1).contains("A peça Fusível está acabando?"));
    }

    @Test
    @DisplayName("snapshot factory falls back to description, brand and model for the appliance")
    void applianceFallbacks() {
        ShopSnapshot shop = ShopSnapshotFactory.from(List.of(
                Order.builder().id(1).status(OrderStatus.PENDING).customerDescription("  Liquidificador sem tampa que não liga  ").build(),
                Order.builder().id(2).status(OrderStatus.PENDING).orderItems(List.of(OrderItem.builder().brand("Philco").build())).build(),
                Order.builder().id(3).status(OrderStatus.PENDING).orderItems(List.of(OrderItem.builder().model("X1").build())).build(),
                Order.builder().id(4).status(null).build(),
                Order.builder().id(5).status(OrderStatus.DELIVERED).build()
        ), null, null, null);

        assertEquals("Liquidificador sem tampa que", shop.openRepairs().get(0).appliance());
        assertEquals("Philco", shop.openRepairs().get(1).appliance());
        assertEquals("X1", shop.openRepairs().get(2).appliance());
        assertEquals(3, shop.openRepairs().size());
        assertTrue(shop.readyForPickup().isEmpty());
        assertNull(shop.lastAnnouncementTitle());
    }
}
