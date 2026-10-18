package com.undercontroll.domain.usecase.insights.impl;

import com.undercontroll.domain.enums.InsightJobStatus;
import com.undercontroll.domain.gateway.InsightJobGateway;
import com.undercontroll.domain.gateway.InsightPublicationGateway;
import com.undercontroll.domain.gateway.InsightsLlmGateway;
import com.undercontroll.domain.gateway.MarketSnapshotGateway;
import com.undercontroll.domain.model.insight.InsightJob;
import com.undercontroll.domain.model.insight.InsightReadState;
import com.undercontroll.domain.model.insight.InsightsPayload;
import com.undercontroll.domain.model.insight.MarketInsightPublication;
import com.undercontroll.domain.model.market.PublishedMarketSnapshot;
import com.undercontroll.domain.usecase.insights.GetMarketInsightsPort.Output;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GetMarketInsightsImplTest {

    private static final Instant PUBLISHED = Instant.parse("2026-10-01T03:00:00Z");
    private static final Instant GENERATED = Instant.parse("2026-09-01T04:00:00Z");
    private static final InsightsPayload PAYLOAD = new InsightsPayload(
            new InsightsPayload.Periodo("2026-08", "2026-07"), new InsightsPayload.CoberturaMatch(1, 0, 0, 0),
            List.of(), List.of(), List.of("Popularidade, não vendas."));

    private final MarketSnapshotGateway snapshots = mock(MarketSnapshotGateway.class);
    private final InsightPublicationGateway publications = mock(InsightPublicationGateway.class);
    private final InsightJobGateway jobs = mock(InsightJobGateway.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<InsightsLlmGateway> llm = mock(ObjectProvider.class);
    private final GetMarketInsightsImpl useCase = new GetMarketInsightsImpl(snapshots, publications, jobs, llm);

    @BeforeEach
    void setUp() {
        when(snapshots.findPublished()).thenReturn(Optional.of(
                new PublishedMarketSnapshot(11L, "run", "2026-09", "SUCCESS", PUBLISHED)));
        when(llm.getIfAvailable()).thenReturn(mock(InsightsLlmGateway.class));
    }

    @Test
    @DisplayName("without a published snapshot the state is AWAITING_DATA and nothing else is read")
    void awaitingData() {
        when(snapshots.findPublished()).thenReturn(Optional.empty());

        assertEquals(Output.awaitingData(), useCase.execute());
        verifyNoInteractions(publications, jobs);
    }

    @Test
    @DisplayName("a publication of the published snapshot is READY with both months and dates")
    void ready() {
        when(publications.findLatest()).thenReturn(Optional.of(publication(11L, "2026-09")));

        Output output = useCase.execute();

        assertEquals(InsightReadState.READY, output.state());
        assertEquals("2026-09", output.requestedBucketKey());
        assertEquals(PUBLISHED, output.snapshotPublishedAt());
        assertEquals("2026-09", output.availableBucketKey());
        assertEquals(GENERATED, output.generatedAt());
        assertSame(PAYLOAD, output.payload());
        verifyNoInteractions(jobs);
    }

    @Test
    @DisplayName("a pending or running job is GENERATING and still serves the previous publication")
    void generating() {
        when(publications.findLatest()).thenReturn(Optional.of(publication(10L, "2026-08")));
        when(jobs.findLatestForSnapshot(11L)).thenReturn(Optional.of(job(InsightJobStatus.RUNNING, null)));

        Output output = useCase.execute();

        assertEquals(InsightReadState.GENERATING, output.state());
        assertEquals("2026-09", output.requestedBucketKey());
        assertEquals("2026-08", output.availableBucketKey());
        assertSame(PAYLOAD, output.payload());
    }

    @Test
    @DisplayName("a failed job is FAILED with the error kind and no payload when nothing was published")
    void failed() {
        when(publications.findLatest()).thenReturn(Optional.empty());
        when(jobs.findLatestForSnapshot(11L)).thenReturn(Optional.of(job(InsightJobStatus.FAILED, "AUTH: 401 Unauthorized")));

        Output output = useCase.execute();

        assertEquals(InsightReadState.FAILED, output.state());
        assertEquals("AUTH", output.failureReason());
        assertNull(output.payload());
        assertNull(output.availableBucketKey());
    }

    @Test
    @DisplayName("before the poller creates the job the state is GENERATING when insights are enabled")
    void notYetEnqueued() {
        when(publications.findLatest()).thenReturn(Optional.empty());
        when(jobs.findLatestForSnapshot(11L)).thenReturn(Optional.empty());

        assertEquals(InsightReadState.GENERATING, useCase.execute().state());
    }

    @Test
    @DisplayName("with insights disabled an older publication is STALE and no publication is FAILED")
    void disabled() {
        when(llm.getIfAvailable()).thenReturn(null);
        when(jobs.findLatestForSnapshot(11L)).thenReturn(Optional.empty());
        when(publications.findLatest()).thenReturn(Optional.of(publication(10L, "2026-08")));

        assertEquals(InsightReadState.STALE, useCase.execute().state());

        when(publications.findLatest()).thenReturn(Optional.empty());
        Output output = useCase.execute();
        assertEquals(InsightReadState.FAILED, output.state());
        assertEquals("INSIGHTS_DISABLED", output.failureReason());
    }

    @Test
    @DisplayName("the failure reason is the error kind before the colon")
    void reason() {
        assertEquals("INVALID_OUTPUT", GetMarketInsightsImpl.reason("INVALID_OUTPUT: still invalid"));
        assertEquals("UNKNOWN", GetMarketInsightsImpl.reason("UNKNOWN"));
        assertNull(GetMarketInsightsImpl.reason(" "));
        assertNull(GetMarketInsightsImpl.reason(null));
    }

    private static MarketInsightPublication publication(long snapshotId, String bucketKey) {
        return new MarketInsightPublication(1L, 1L, 1L, snapshotId, bucketKey, null, PAYLOAD, "gemini", "m", "7", GENERATED);
    }

    private static InsightJob job(InsightJobStatus status, String error) {
        return new InsightJob(5L, 11L, "2026-09", "7", "h", "gemini", "m", status, 1, null, null, PUBLISHED, error,
                PUBLISHED, PUBLISHED, null);
    }
}
