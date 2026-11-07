package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.model.dashboard.ApplianceCount;
import com.undercontroll.domain.model.dashboard.ComponentUsage;
import com.undercontroll.domain.model.dashboard.CustomerTypePoint;
import com.undercontroll.domain.model.dashboard.RevenuePoint;
import com.undercontroll.domain.model.dashboard.StatusCount;
import com.undercontroll.domain.model.market.RepairCatalogRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderReportRowsTest {

    @Test
    @DisplayName("revenue rows convert SQL dates and numeric types, defaulting nulls to zero")
    void revenue() {
        assertEquals(new RevenuePoint(LocalDate.of(2026, 1, 10), 300.5, 120.0, 4L),
                OrderReportRows.revenue(new Object[]{Date.valueOf("2026-01-10"), new BigDecimal("300.50"), 120, 4L}));
        assertEquals(new RevenuePoint(LocalDate.of(2026, 1, 11), 0.0, 1.5, 0L),
                OrderReportRows.revenue(new Object[]{LocalDate.of(2026, 1, 11), null, 1.5d, null}));
        assertEquals(new RevenuePoint(null, 0.0, 0.0, 0L),
                OrderReportRows.revenue(new Object[]{"bad", "x", "y", new Object()}));
    }

    @Test
    @DisplayName("customer type and status rows convert counts")
    void customerTypeAndStatus() {
        assertEquals(new CustomerTypePoint(LocalDate.of(2026, 1, 10), 2L, 1L),
                OrderReportRows.customerType(new Object[]{Date.valueOf("2026-01-10"), 2L, 1}));
        assertEquals(new StatusCount(OrderStatus.PENDING, 5L), OrderReportRows.statusCount(new Object[]{"PENDING", 5}));
    }

    @Test
    @DisplayName("ranking rows convert ids and quantities")
    void rankings() {
        assertEquals(new ApplianceCount("TV", "LG", 7L), OrderReportRows.appliance(new Object[]{"TV", "LG", 7}));
        assertEquals(new ComponentUsage(3, "Capacitor", "Epcos", "Elétrica", 12L),
                OrderReportRows.componentUsage(new Object[]{3L, "Capacitor", "Epcos", "Elétrica", new BigDecimal("12")}));
        assertEquals(new ComponentUsage(null, "Fusível", null, null, 2L),
                OrderReportRows.componentUsage(new Object[]{null, "Fusível", null, null, 2}));
    }

    @Test
    @DisplayName("repair catalog rows tolerate missing columns and textual volumes")
    void repairCatalog() {
        assertEquals(new RepairCatalogRow("Electrolux", "ME23S", "MICROONDAS", 12L),
                OrderReportRows.repairCatalog(new Object[]{"Electrolux", "ME23S", "MICROONDAS", 12L}));
        assertEquals(new RepairCatalogRow("Philco", null, null, 3L),
                OrderReportRows.repairCatalog(new Object[]{"Philco", null, null, "3"}));
        assertEquals(new RepairCatalogRow("Mondial", null, null, 0L),
                OrderReportRows.repairCatalog(new Object[]{"Mondial"}));
    }
}
