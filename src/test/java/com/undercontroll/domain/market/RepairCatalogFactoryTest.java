package com.undercontroll.domain.market;

import com.undercontroll.domain.model.market.RepairCatalogItem;
import com.undercontroll.domain.model.market.RepairCatalogRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepairCatalogFactoryTest {

    @Test
    @DisplayName("builds slug and product_key from order_item rows")
    void fromRows() {
        List<RepairCatalogItem> items = RepairCatalogFactory.fromRows(
                List.of(new RepairCatalogRow("Electrolux", "ME23S", "Microondas", 3L)), List.of());

        assertEquals(1, items.size());
        assertEquals("electrolux", items.get(0).brandSlug());
        assertEquals("electrolux:me23s", items.get(0).productKey());
        assertEquals("MLB-MICROWAVES", items.get(0).domainId());
        assertEquals(3L, items.get(0).volume());
    }

    @Test
    @DisplayName("returns empty list when there are no rows")
    void emptyRows() {
        assertTrue(RepairCatalogFactory.fromRows(List.of(), List.of()).isEmpty());
        assertTrue(RepairCatalogFactory.fromRows(null, List.of()).isEmpty());
    }
}
