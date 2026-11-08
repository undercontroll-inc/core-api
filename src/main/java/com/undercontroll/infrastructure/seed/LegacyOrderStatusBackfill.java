package com.undercontroll.infrastructure.seed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "false", matchIfMissing = true)
public class LegacyOrderStatusBackfill implements ApplicationRunner {

    static final List<String> STATEMENTS = List.of(
            "UPDATE \"orders\" SET version = 0 WHERE version IS NULL",
            "UPDATE \"orders\" SET deadline = completed_time WHERE deadline IS NULL AND completed_time IS NOT NULL",
            "UPDATE \"orders\" SET status = 'RECEIVED' WHERE status IN ('PENDING', 'IN_ANALYSIS')",
            "UPDATE \"orders\" SET status = 'READY_FOR_PICKUP', outcome = 'REPAIRED', completed_at = COALESCE(completed_at, updated_at) WHERE status = 'COMPLETED'",
            "UPDATE \"orders\" SET outcome = 'REPAIRED', completed_at = COALESCE(completed_at, updated_at), delivered_at = COALESCE(delivered_at, updated_at) WHERE status = 'DELIVERED' AND outcome IS NULL"
    );

    private final JdbcTemplate jdbc;

    public LegacyOrderStatusBackfill(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        int changed = 0;
        for (String statement : STATEMENTS) {
            try {
                changed += jdbc.update(statement);
            } catch (RuntimeException ex) {
                log.debug("Legacy order backfill statement skipped cause={}", ex.toString());
            }
        }
        if (changed > 0) {
            log.info("Legacy order statuses backfilled rows={}", changed);
        }
    }
}
