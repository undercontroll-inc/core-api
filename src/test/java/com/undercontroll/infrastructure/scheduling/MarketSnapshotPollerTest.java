package com.undercontroll.infrastructure.scheduling;

import com.undercontroll.domain.enums.InsightJobStatus;
import com.undercontroll.domain.model.insight.InsightJob;
import com.undercontroll.domain.usecase.insights.EnqueueInsightJobPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarketSnapshotPollerTest {

    private final EnqueueInsightJobPort enqueue = mock(EnqueueInsightJobPort.class);
    private final ConcurrentMapCacheManager caches = new ConcurrentMapCacheManager("marketAnalytics", "marketInsights");
    private final MarketSnapshotPoller poller = new MarketSnapshotPoller(enqueue, caches);

    @Test
    @DisplayName("a newly published snapshot enqueues its job and clears the market caches once")
    void newSnapshotClearsCaches() {
        when(enqueue.execute(false)).thenReturn(enqueued(10L));
        poller.poll();
        cacheEntries();

        poller.poll();
        assertNotNull(caches.getCache("marketAnalytics").get("current"));

        when(enqueue.execute(false)).thenReturn(enqueued(11L));
        poller.poll();
        assertNull(caches.getCache("marketAnalytics").get("current"));
        assertNull(caches.getCache("marketInsights").get("current"));
    }

    @Test
    @DisplayName("awaiting data or a failing poll leaves the caches alone")
    void nothingToDo() {
        cacheEntries();
        when(enqueue.execute(false)).thenReturn(EnqueueInsightJobPort.Result.awaitingData());
        poller.poll();
        assertNotNull(caches.getCache("marketInsights").get("current"));

        when(enqueue.execute(false)).thenThrow(new IllegalStateException("db down"));
        assertDoesNotThrow(poller::poll);
        assertEquals("cached", caches.getCache("marketInsights").get("current").get());
    }

    private void cacheEntries() {
        caches.getCache("marketAnalytics").put("current", "cached");
        caches.getCache("marketInsights").put("current", "cached");
    }

    private static EnqueueInsightJobPort.Result enqueued(long snapshotId) {
        Instant now = Instant.parse("2026-09-02T04:00:00Z");
        return new EnqueueInsightJobPort.Result(EnqueueInsightJobPort.Result.Status.ENQUEUED,
                new InsightJob(1L, snapshotId, "2026-08", "7", "h", "gemini", "m", InsightJobStatus.PENDING, 0,
                        null, null, now, null, now, now, null));
    }
}
