package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.enums.InsightAttemptStatus;
import com.undercontroll.domain.enums.InsightJobStatus;
import com.undercontroll.domain.exception.InsightLeaseLostException;
import com.undercontroll.domain.model.insight.InsightAttempt;
import com.undercontroll.domain.model.insight.InsightJob;
import com.undercontroll.domain.model.insight.InsightsPayload;
import com.undercontroll.domain.model.insight.MarketInsightPublication;
import com.undercontroll.domain.model.insight.NewInsightJob;
import com.undercontroll.infrastructure.mapper.InsightJobPersistenceMapperImpl;
import com.undercontroll.infrastructure.mapper.InsightsPayloadCodec;
import com.undercontroll.infrastructure.mapper.InsightsPayloadJsonMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.AfterEach;
import com.undercontroll.infrastructure.persistence.repository.InsightAttemptJpaRepository;
import com.undercontroll.infrastructure.persistence.repository.InsightJobJpaRepository;
import com.undercontroll.infrastructure.persistence.repository.MarketInsightPublicationJpaRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = false)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import({
        InsightJobGatewayImpl.class,
        InsightJobPersistenceMapperImpl.class,
        InsightsPayloadCodec.class,
        InsightsPayloadJsonMapperImpl.class
})
class InsightJobGatewayImplTest {

    private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");
    private static final Instant LEASE = NOW.plus(Duration.ofMinutes(5));

    @Autowired
    private InsightJobGatewayImpl gateway;

    @Autowired
    private InsightJobJpaRepository jobs;

    @Autowired
    private InsightAttemptJpaRepository attempts;

    @Autowired
    private MarketInsightPublicationJpaRepository publications;

    @AfterEach
    void cleanUp() {
        publications.deleteAll();
        attempts.deleteAll();
        jobs.deleteAll();
    }

    @Test
    @DisplayName("enqueue is idempotent per snapshot, prompt version and model config hash")
    void enqueueIsIdempotent() {
        InsightJob first = gateway.enqueue(newJob(10L, "h1"), NOW);
        InsightJob again = gateway.enqueue(newJob(10L, "h1"), NOW.plusSeconds(60));
        InsightJob otherConfig = gateway.enqueue(newJob(10L, "h2"), NOW.plusSeconds(120));

        assertEquals(first.id(), again.id());
        assertEquals(InsightJobStatus.PENDING, first.status());
        assertEquals(NOW, first.nextRunAt());
        assertTrue(otherConfig.id() > first.id());
        assertEquals(otherConfig.id(), gateway.findLatestForSnapshot(10L).orElseThrow().id());
        assertTrue(gateway.findLatestForSnapshot(99L).isEmpty());
    }

    @Test
    @DisplayName("only one worker acquires a pending job and an expired lease can be taken over")
    void leaseByConditionalUpdate() {
        long jobId = gateway.enqueue(newJob(11L, "h"), NOW).id();

        InsightJob acquired = gateway.tryAcquire(jobId, "worker-a", NOW, LEASE).orElseThrow();

        assertEquals(InsightJobStatus.RUNNING, acquired.status());
        assertEquals("worker-a", acquired.leaseOwner());
        assertEquals(1, acquired.attemptCount());
        assertTrue(gateway.tryAcquire(jobId, "worker-b", NOW.plusSeconds(10), LEASE).isEmpty());
        assertEquals(List.of(), gateway.findRunnable(NOW.plusSeconds(10), 10));

        Instant afterLease = LEASE.plusSeconds(1);
        assertEquals(List.of(jobId), gateway.findRunnable(afterLease, 10));
        InsightJob takenOver = gateway.tryAcquire(jobId, "worker-b", afterLease, afterLease.plusSeconds(300)).orElseThrow();
        assertEquals("worker-b", takenOver.leaseOwner());
        assertEquals(2, takenOver.attemptCount());
        assertFalse(gateway.reschedule(jobId, "worker-a", NOW, "late", NOW));
    }

    @Test
    @DisplayName("pending jobs are not runnable before next_run_at")
    void runnableHonoursNextRunAt() {
        long jobId = gateway.enqueue(newJob(12L, "h"), NOW).id();
        gateway.tryAcquire(jobId, "w", NOW, LEASE);

        assertTrue(gateway.reschedule(jobId, "w", NOW.plusSeconds(600), "rate limited", NOW));

        InsightJob rescheduled = gateway.findById(jobId).orElseThrow();
        assertEquals(InsightJobStatus.PENDING, rescheduled.status());
        assertNull(rescheduled.leaseOwner());
        assertEquals("rate limited", rescheduled.lastError());
        assertTrue(gateway.findRunnable(NOW.plusSeconds(599), 10).isEmpty());
        assertTrue(gateway.tryAcquire(jobId, "w", NOW.plusSeconds(599), LEASE).isEmpty());
        assertEquals(List.of(jobId), gateway.findRunnable(NOW.plusSeconds(600), 10));
    }

    @Test
    @DisplayName("failed jobs keep the error and can be requeued with a fresh attempt budget")
    void failAndRequeue() {
        long jobId = gateway.enqueue(newJob(13L, "h"), NOW).id();
        gateway.tryAcquire(jobId, "w", NOW, LEASE);

        assertTrue(gateway.fail(jobId, "w", "AUTH: 401", NOW));
        InsightJob failed = gateway.findById(jobId).orElseThrow();
        assertEquals(InsightJobStatus.FAILED, failed.status());
        assertEquals(NOW, failed.finishedAt());
        assertFalse(failed.active());

        assertTrue(gateway.requeue(jobId, NOW.plusSeconds(5)));
        assertFalse(gateway.requeue(jobId, NOW.plusSeconds(6)));
        InsightJob requeued = gateway.findById(jobId).orElseThrow();
        assertEquals(InsightJobStatus.PENDING, requeued.status());
        assertEquals(0, requeued.attemptCount());
        assertNull(requeued.lastError());
        assertTrue(requeued.active());
    }

    @Test
    @DisplayName("publish stores the payload, completes the job and the attempt in one transaction")
    void publish() {
        long jobId = gateway.enqueue(newJob(14L, "h"), NOW).id();
        gateway.tryAcquire(jobId, "w", NOW, LEASE);
        InsightAttempt attempt = gateway.saveAttempt(InsightAttempt.start(jobId, 1, NOW));
        InsightAttempt valid = gateway.saveAttempt(new InsightAttempt(attempt.id(), jobId, 1, InsightAttemptStatus.VALID,
                "{\"insights\":[]}", null, null, null, 1, NOW, null));

        MarketInsightPublication published = gateway.publish(publication(jobId, valid.id(), 14L), "w", NOW.plusSeconds(30));

        assertEquals("2026-09", published.bucketKey());
        assertEquals(InsightJobStatus.SUCCESS, gateway.findById(jobId).orElseThrow().status());
        InsightAttempt stored = gateway.findLatestAttempt(jobId).orElseThrow();
        assertEquals(InsightAttemptStatus.PUBLISHED, stored.status());
        assertEquals("{\"insights\":[]}", stored.draft());
        MarketInsightPublication latest = gateway.findLatest().orElseThrow();
        assertEquals(published.id(), latest.id());
        assertEquals(List.of("Popularidade, não vendas."), latest.payload().limitacoes());
    }

    @Test
    @DisplayName("publish without the lease fails and stores nothing")
    void publishWithoutLease() {
        long jobId = gateway.enqueue(newJob(15L, "h"), NOW).id();
        gateway.tryAcquire(jobId, "w", NOW, LEASE);
        InsightAttempt attempt = gateway.saveAttempt(InsightAttempt.start(jobId, 1, NOW));

        assertThrows(InsightLeaseLostException.class,
                () -> gateway.publish(publication(jobId, attempt.id(), 15L), "intruder", NOW));
        assertTrue(gateway.findLatest().isEmpty());
        assertEquals(InsightJobStatus.RUNNING, gateway.findById(jobId).orElseThrow().status());
    }

    private static NewInsightJob newJob(long snapshotId, String hash) {
        return new NewInsightJob(snapshotId, "2026-09", "7", hash, "gemini", "gemini-2.5-flash");
    }

    private static MarketInsightPublication publication(long jobId, long attemptId, long snapshotId) {
        InsightsPayload payload = new InsightsPayload(
                new InsightsPayload.Periodo("2026-09", "2026-08"),
                new InsightsPayload.CoberturaMatch(1, 0, 0, 2),
                List.of(),
                List.of(),
                List.of("Popularidade, não vendas.")
        );
        return new MarketInsightPublication(null, jobId, attemptId, snapshotId, "2026-09", "2026-08", payload,
                "gemini", "gemini-2.5-flash", "7", NOW);
    }
}
