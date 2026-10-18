package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.gateway.MarketViewGateway;
import com.undercontroll.domain.model.insight.InsightPromptContext;
import com.undercontroll.domain.model.market.MarketBrandSummary;
import com.undercontroll.domain.model.market.MarketCategorySummary;
import com.undercontroll.domain.model.market.MarketPriceMovement;
import com.undercontroll.domain.model.market.MatchCoverage;
import com.undercontroll.domain.model.market.RepairCatalogItem;
import com.undercontroll.domain.model.market.RepairCatalogLine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.annotation.Tool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsightGroundingBuilderTest {

    @Mock
    private MarketViewGateway marketViewGateway;

    @InjectMocks
    private InsightGroundingBuilder tools;

    @Test
    @DisplayName("grounding loads required facts and deepening blocks in one pass")
    void usesContext() {
        InsightPromptContext prompt = new InsightPromptContext(
                "2026-08",
                "2026-07",
                new MatchCoverage(2, 1, 0, 3),
                Set.of("MLB-MICROWAVES"),
                List.of(
                        new RepairCatalogItem("Philco", "PMO23", "MICROONDAS", 5, "philco", "pmo23", "MLB-MICROWAVES"),
                        new RepairCatalogItem("Electrolux", "ME23S", "MICROONDAS", 12, "electrolux", "me23s", "MLB-MICROWAVES"),
                        new RepairCatalogItem("Mondial", "AF-30", "AIRFRYER", 8, "mondial", "af-30", "MLB-AIR_FRYERS")
                ),
                List.of(new MarketCategorySummary(
                        "MLB-AIR_FRYERS", "2026-08", "Air fryer", 8L, 3L, 75.0, 88.0,
                        299.0, 200.0, 400.0, 5.0, -3.0, 2.0, 1L, 0L, 1L))
        );
        EvidenceIndex evidence = new EvidenceIndex(new com.fasterxml.jackson.databind.ObjectMapper());
        InsightGenerationContext.State state = InsightGenerationContext.State.from(prompt, evidence);

        when(marketViewGateway.countCurrentProducts()).thenReturn(20L);
        when(marketViewGateway.countDistinctBrands()).thenReturn(8L);
        when(marketViewGateway.findTopBrands("2026-08", 5)).thenReturn(List.of(
                new MarketBrandSummary(
                        "electrolux", "2026-08", "Electrolux", 10L, 2L, 80.0, 1, 599.0,
                        400.0, 800.0, 2.0, 5.0, 3L)
        ));
        when(marketViewGateway.findTopCategories("2026-08", 5)).thenReturn(List.of(
                new MarketCategorySummary(
                        "MLB-MICROWAVES", "2026-08", "Micro-ondas", 12L, 4L, 70.0, 90.0,
                        599.0, 400.0, 800.0, 6.0, 1.0, 4.0, 2L, 1L, 2L)
        ));
        when(marketViewGateway.findPriceMovements("2026-08")).thenReturn(List.of(
                new MarketPriceMovement("MLB-MICROWAVES", "2026-08", "2026-07", 4L, 10L, 3L, 599.0, 540.0, 10.9, 2.0)
        ));
        when(marketViewGateway.findStockOpportunities("2026-08")).thenReturn(List.of());
        when(marketViewGateway.findRisingProductsHighConfidence()).thenReturn(List.of());
        when(marketViewGateway.findBrandMomentum("2026-08")).thenReturn(List.of());
        when(marketViewGateway.findPriceDispersion()).thenReturn(List.of());
        when(marketViewGateway.findUncoveredCategories("2026-08", Set.of("MLB-MICROWAVES"))).thenReturn(List.of());

        InsightGrounding grounding = tools.build(state);
        assertEquals(2, grounding.coberturaMatch().nivel1Exato());
        assertEquals(3, grounding.repairCatalog().size());
        assertEquals("Electrolux", grounding.repairCatalog().get(0).brand());
        assertEquals(12, grounding.repairCatalog().get(0).volume());
        assertEquals(2, grounding.repairMix().size());
        assertEquals("MLB-MICROWAVES", grounding.repairMix().get(0).domainId());
        assertEquals(17, grounding.repairMix().get(0).volume());
        assertEquals(68.0, grounding.repairMix().get(0).sharePct());
        assertEquals(20L, grounding.marketSnapshot().totalProducts());
        assertEquals(8L, grounding.marketSnapshot().brandsAnalyzed());
        assertEquals("Electrolux", grounding.marketSnapshot().topBrands().get(0).name());
        assertEquals("Micro-ondas", grounding.marketSnapshot().topCategories().get(0).name());
        assertEquals(1, grounding.priceMovements().size());
        assertEquals(10.9, grounding.priceMovements().get(0).priceDeltaPct());
        assertEquals("Air fryer", grounding.categorySummary().get(0).categoryName());
        verify(marketViewGateway, never()).findCategorySummary(any());
        assertTrue(evidence.contains(12));
        assertTrue(evidence.contains(20));
        assertTrue(evidence.contains(10.9));
    }

    @Test
    @DisplayName("repair catalog grounding caps at 25 rows by volume")
    void capsRepairCatalog() {
        List<RepairCatalogItem> items = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            items.add(new RepairCatalogItem(
                    "Marca" + i, "M" + i, "TIPO", i, "marca" + i, "m" + i, "MLB-X"));
        }
        InsightPromptContext prompt = new InsightPromptContext(
                "2026-08", "2026-07", MatchCoverage.empty(), Set.of(), items, List.of());
        InsightGenerationContext.State state = InsightGenerationContext.State.from(
                prompt, new EvidenceIndex(new com.fasterxml.jackson.databind.ObjectMapper()));

        List<RepairCatalogLine> catalog = tools.repairCatalog(state);
        assertEquals(25, catalog.size());
        assertEquals(30, catalog.get(0).volume());
        assertEquals(6, catalog.get(24).volume());
    }

    @Test
    @DisplayName("does not expose ChatClient tools")
    void noChatTools() {
        Set<String> names = Arrays.stream(InsightGroundingBuilder.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Tool.class))
                .map(method -> method.getAnnotation(Tool.class).name())
                .collect(Collectors.toSet());
        assertTrue(names.isEmpty());
    }

    @Test
    @DisplayName("caps price movements before sending them to the model")
    void capsPriceMovements() {
        InsightPromptContext prompt = new InsightPromptContext(
                "2026-08", "2026-07", MatchCoverage.empty(), Set.of(), List.of(), List.of());
        InsightGenerationContext.State state = InsightGenerationContext.State.from(
                prompt, new EvidenceIndex(new com.fasterxml.jackson.databind.ObjectMapper()));
        List<MarketPriceMovement> rows = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            rows.add(new MarketPriceMovement("MLB-X", "2026-08", "2026-07", 2L, 3L, 1L, 10.0 + i, 9.0, 1.0, 0.0));
        }
        when(marketViewGateway.findPriceMovements("2026-08")).thenReturn(rows);

        assertEquals(15, tools.priceMovements(state).size());
    }
}
