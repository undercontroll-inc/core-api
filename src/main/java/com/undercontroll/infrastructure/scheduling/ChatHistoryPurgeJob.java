package com.undercontroll.infrastructure.scheduling;

import com.undercontroll.domain.gateway.ConversationGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

@Slf4j
@Component
public class ChatHistoryPurgeJob {

    private final ConversationGateway conversations;
    private final Clock clock;
    private final Duration retention;

    public ChatHistoryPurgeJob(
            ConversationGateway conversations,
            Clock clock,
            @Value("${undercontroll.ai.ana.deleted-retention:30d}") Duration retention
    ) {
        this.conversations = conversations;
        this.clock = clock;
        this.retention = retention;
    }

    @Scheduled(cron = "${undercontroll.ai.ana.purge-cron:0 15 3 * * *}")
    public void purge() {
        try {
            int purged = 0;
            int batch;
            do {
                batch = conversations.purgeDeleted(clock.instant().minus(retention));
                purged += batch;
            } while (batch > 0);
            if (purged > 0) {
                log.info("Purged {} deleted conversation(s)", purged);
            }
        } catch (RuntimeException ex) {
            log.warn("Deleted conversation purge failed: {}", ex.toString());
        }
    }
}
