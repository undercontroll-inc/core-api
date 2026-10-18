package com.undercontroll.domain.market;

import com.undercontroll.domain.model.market.MarketProductCurrent;
import com.undercontroll.domain.model.market.MatchCoverage;
import com.undercontroll.domain.model.market.RepairCatalogItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepairCatalogMatcherTest {

    @Test
    @DisplayName("counts each product once at the most precise match level")
    void coverageLevels() {
        List<MarketProductCurrent> products = List.of(
                product("electrolux:me23s", "electrolux", "MLB-MICROWAVES"),
                product("lg:ms3052r", "lg", "MLB-MICROWAVES"),
                product("mondial:af-14", "mondial", "MLB-AIR_FRYERS"),
                product("philco:xyz", "philco", "MLB-FANS")
        );
        List<RepairCatalogItem> catalog = List.of(
                new RepairCatalogItem("Electrolux", "ME23S", "Microondas", 4,
                        "electrolux", "electrolux:me23s", "MLB-MICROWAVES"),
                new RepairCatalogItem("LG", "OTHER", "Microondas", 1,
                        "lg", "lg:other", "MLB-MICROWAVES")
        );

        MatchCoverage coverage = RepairCatalogMatcher.coverage(products, catalog);

        assertEquals(1, coverage.exact());
        assertEquals(1, coverage.brandCategory());
        assertEquals(0, coverage.category());
        assertEquals(2, coverage.none());
        assertEquals(1, RepairCatalogMatcher.clientDomainIds(catalog).size());
    }

    @Test
    @DisplayName("empty products yield empty coverage")
    void emptyProducts() {
        MatchCoverage coverage = RepairCatalogMatcher.coverage(List.of(), List.of());
        assertEquals(MatchCoverage.empty(), coverage);
    }

    private static MarketProductCurrent product(String productKey, String brandSlug, String domainId) {
        return new MarketProductCurrent(
                "2026-08", "HIGHLIGHTS_CATEGORY", "PRODUCT", "id", "title", "title",
                1, "MUITO_ALTO", 1, "CRESCIMENTO", 80.0, "PRIORIDADE_ALTA", "HIGH",
                domainId, "MLB1", "Cat", "Brand", brandSlug, "M", productKey,
                "110V", 1000, 20.0, "L", "A", 100.0, 90.0, 100.0, 110.0, 1.0, 10.0,
                null, 5, 5, false, 3, 0.9, "DOMINIO_WHITELIST", "http://x"
        );
    }
}
