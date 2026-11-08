package com.undercontroll.infrastructure.seed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DataJpaTest(showSql = false)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LegacyOrderStatusBackfillTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("maps legacy statuses, copies the deadline and is idempotent")
    void backfills() {
        jdbc.execute("ALTER TABLE \"orders\" ADD COLUMN IF NOT EXISTS completed_time DATE");
        jdbc.execute("ALTER TABLE \"orders\" ALTER COLUMN version SET NULL");
        jdbc.execute("ALTER TABLE \"orders\" ALTER COLUMN status SET DATA TYPE VARCHAR(50)");
                jdbc.update("DELETE FROM \"orders\"");
        insert(1, "PENDING");
        insert(2, "IN_ANALYSIS");
        insert(3, "COMPLETED");
        insert(4, "DELIVERED");

        LegacyOrderStatusBackfill backfill = new LegacyOrderStatusBackfill(jdbc);
        backfill.run(null);
        backfill.run(null);

        assertEquals("RECEIVED", row(1).get("STATUS"));
        assertEquals("RECEIVED", row(2).get("STATUS"));
        assertNull(row(1).get("OUTCOME"));
        assertEquals("READY_FOR_PICKUP", row(3).get("STATUS"));
        assertEquals("REPAIRED", row(3).get("OUTCOME"));
        assertNotNull(row(3).get("COMPLETED_AT"));
        assertEquals("DELIVERED", row(4).get("STATUS"));
        assertEquals("REPAIRED", row(4).get("OUTCOME"));
        assertNotNull(row(4).get("DELIVERED_AT"));
        assertEquals(java.sql.Date.valueOf("2026-01-20"), row(1).get("DEADLINE"));
        assertEquals(0L, ((Number) row(1).get("VERSION")).longValue());
        jdbc.update("DELETE FROM \"orders\"");
    }

    private void insert(int id, String status) {
        jdbc.update("INSERT INTO \"orders\" (id, status, completed_time, updated_at, fabric_guarantee, return_guarantee, version) "
                + "VALUES (?, ?, DATE '2026-01-20', TIMESTAMP '2026-01-21 10:00:00', false, false, NULL)", id, status);
    }

    private Map<String, Object> row(int id) {
        return jdbc.queryForMap("SELECT * FROM \"orders\" WHERE id = ?", id);
    }
}
