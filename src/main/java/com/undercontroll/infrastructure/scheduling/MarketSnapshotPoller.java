package com.undercontroll.infrastructure.scheduling;

import com.undercontroll.domain.usecase.insights.EnqueueInsightJobPort;
import com.undercontroll.infrastructure.logging.MdcKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketSnapshotPoller {

    static final List<String> MARKET_CACHES = List.of("marketAnalytics", "marketInsights");

    private final EnqueueInsightJobPort enqueueInsightJobPort;
    private final CacheManager cacheManager;
    private volatile Long lastSnapshotId;

    @Scheduled(
            fixedDelayString = "${undercontroll.ai.insights.poll-interval:5m}",
            initialDelayString = "${undercontroll.ai.insights.poll-initial-delay:30s}"
    )
    public void poll() {
        MDC.put(MdcKeys.CORRELATION_ID, "insights-poll-" + UUID.randomUUID());
        try {
            EnqueueInsightJobPort.Result result = enqueueInsightJobPort.execute(false);
            if (result.job() == null) {
                log.debug("Market snapshot poll status={}", result.status());
                return;
            }
            long snapshotId = result.job().snapshotId();
            if (!Objects.equals(lastSnapshotId, snapshotId)) {
                MARKET_CACHES.stream().map(cacheManager::getCache).filter(Objects::nonNull).forEach(Cache::clear);
                log.info("Market snapshot {} detected bucketKey={} jobId={} jobStatus={}",
                        snapshotId, result.job().bucketKey(), result.job().id(), result.job().status());
                lastSnapshotId = snapshotId;
            }
        } catch (RuntimeException ex) {
            log.warn("Market snapshot poll failed: {}", ex.toString());
        } finally {
            MDC.clear();
        }
    }
}
