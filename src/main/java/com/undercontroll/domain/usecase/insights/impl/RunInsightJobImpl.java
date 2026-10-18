package com.undercontroll.domain.usecase.insights.impl;

import com.undercontroll.domain.enums.InsightAttemptStatus;
import com.undercontroll.domain.exception.InsightLeaseLostException;
import com.undercontroll.domain.exception.InsightLlmException;
import com.undercontroll.domain.exception.MarketDataUnavailableException;
import com.undercontroll.domain.gateway.InsightDraftCodec;
import com.undercontroll.domain.gateway.InsightJobGateway;
import com.undercontroll.domain.gateway.InsightsLlmGateway;
import com.undercontroll.domain.gateway.MarketSnapshotGateway;
import com.undercontroll.domain.gateway.MarketViewGateway;
import com.undercontroll.domain.insight.InsightContract;
import com.undercontroll.domain.insight.InsightDraftValidator;
import com.undercontroll.domain.insight.InsightInputs;
import com.undercontroll.domain.insight.InsightMarketDataLoader;
import com.undercontroll.domain.insight.InsightNarrativeRenderer;
import com.undercontroll.domain.insight.InsightRetryPolicy;
import com.undercontroll.domain.model.insight.InsightAttempt;
import com.undercontroll.domain.model.insight.InsightDraft;
import com.undercontroll.domain.model.insight.InsightDraftRequest;
import com.undercontroll.domain.model.insight.InsightGenerationInput;
import com.undercontroll.domain.model.insight.InsightGenerationSettings;
import com.undercontroll.domain.model.insight.InsightJob;
import com.undercontroll.domain.model.insight.InsightJobSettings;
import com.undercontroll.domain.model.insight.LlmFailure;
import com.undercontroll.domain.model.insight.MarketInsightPublication;
import com.undercontroll.domain.model.market.PublishedMarketSnapshot;
import com.undercontroll.domain.support.Deadline;
import com.undercontroll.domain.support.Durations;
import com.undercontroll.domain.support.Sleeper;
import com.undercontroll.domain.usecase.insights.RunInsightJobPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

@Slf4j
@Service
public class RunInsightJobImpl implements RunInsightJobPort {

    static final int MAX_ERROR_LENGTH = 1000;

    private final MarketSnapshotGateway marketSnapshotGateway;
    private final MarketViewGateway marketViewGateway;
    private final InsightMarketDataLoader insightMarketDataLoader;
    private final InsightJobGateway insightJobGateway;
    private final InsightDraftCodec insightDraftCodec;
    private final ObjectProvider<InsightsLlmGateway> insightsLlmGateway;
    private final InsightGenerationSettings generationSettings;
    private final InsightJobSettings jobSettings;
    private final InsightContract contract;
    private final InsightRetryPolicy retryPolicy;
    private final Clock clock;
    private final Sleeper sleeper;
    private final RandomGenerator random;

    public RunInsightJobImpl(
            MarketSnapshotGateway marketSnapshotGateway,
            MarketViewGateway marketViewGateway,
            InsightMarketDataLoader insightMarketDataLoader,
            InsightJobGateway insightJobGateway,
            InsightDraftCodec insightDraftCodec,
            ObjectProvider<InsightsLlmGateway> insightsLlmGateway,
            InsightGenerationSettings generationSettings,
            InsightJobSettings jobSettings,
            InsightContract contract,
            InsightRetryPolicy retryPolicy,
            Clock clock,
            Sleeper sleeper,
            RandomGenerator random
    ) {
        this.marketSnapshotGateway = marketSnapshotGateway;
        this.marketViewGateway = marketViewGateway;
        this.insightMarketDataLoader = insightMarketDataLoader;
        this.insightJobGateway = insightJobGateway;
        this.insightDraftCodec = insightDraftCodec;
        this.insightsLlmGateway = insightsLlmGateway;
        this.generationSettings = generationSettings;
        this.jobSettings = jobSettings;
        this.contract = contract;
        this.retryPolicy = retryPolicy;
        this.clock = clock;
        this.sleeper = sleeper;
        this.random = random;
    }

    @Override
    @CacheEvict(value = "marketInsights", allEntries = true)
    public Outcome execute(InsightJob job, String owner) {
        long started = System.nanoTime();
        Deadline deadline = Deadline.after(jobSettings.deadline());
        Run run = new Run(job, owner);
        Outcome outcome = run.execute(deadline);
        log.info("Insight job finished jobId={} snapshotId={} attempt={} outcome={} durationMs={}",
                job.id(), job.snapshotId(), job.attemptCount(), outcome, Durations.millisSince(started));
        return outcome;
    }

    private final class Run {

        private final InsightJob job;
        private final String owner;
        private InsightAttempt attempt;
        private int llmCalls;
        private Outcome terminal;

        private Run(InsightJob job, String owner) {
            this.job = job;
            this.owner = owner;
        }

        Outcome execute(Deadline deadline) {
            Optional<PublishedMarketSnapshot> snapshot = marketSnapshotGateway.findPublished();
            if (snapshot.isEmpty() || snapshot.get().snapshotId() != job.snapshotId()) {
                return fail("SUPERSEDED", "Snapshot " + job.snapshotId() + " is no longer the published snapshot");
            }
            InsightsLlmGateway llm = insightsLlmGateway.getIfAvailable();
            if (llm == null) {
                return fail(LlmFailure.Kind.CONFIG.name(), "Insights provider is disabled");
            }
            InsightGenerationInput input;
            try {
                input = InsightInputs.build(
                        insightMarketDataLoader.load(job.bucketKey(), generationSettings.repairCatalogDays()),
                        marketViewGateway.findAllCurrentProducts(),
                        snapshot.get().partial()
                );
            } catch (MarketDataUnavailableException ex) {
                return retryLater("MARKET_DATA", ex.getMessage(), null);
            }
            try {
                InsightDraft draft = reusableDraft(input).orElseGet(() -> generate(llm, input, deadline));
                if (draft == null) {
                    return terminal;
                }
                return publish(draft, input);
            } catch (InsightLlmException ex) {
                return llmFailure(ex);
            } catch (RuntimeException ex) {
                recordAttemptFailure("UNKNOWN", ex.toString());
                return retryLater("UNKNOWN", ex.toString(), null);
            }
        }

        private Optional<InsightDraft> reusableDraft(InsightGenerationInput input) {
            Optional<InsightDraft> stored = insightJobGateway.findLatestAttempt(job.id())
                    .filter(previous -> previous.status() == InsightAttemptStatus.VALID && previous.draft() != null)
                    .map(previous -> insightDraftCodec.decode(previous.draft()))
                    .filter(draft -> InsightDraftValidator.validate(draft, input.candidates(), contract).isEmpty());
            stored.ifPresent(draft -> {
                attempt = insightJobGateway.saveAttempt(new InsightAttempt(null, job.id(), job.attemptCount(),
                        InsightAttemptStatus.VALID, insightDraftCodec.encode(draft), null, null, null, 0,
                        now(), null));
                log.info("Reusing validated insight draft jobId={} attempt={}", job.id(), job.attemptCount());
            });
            return stored;
        }

        private InsightDraft generate(InsightsLlmGateway llm, InsightGenerationInput input, Deadline deadline) {
            attempt = insightJobGateway.saveAttempt(InsightAttempt.start(job.id(), job.attemptCount(), now()));
            if (input.candidates().isEmpty()) {
                InsightDraft empty = new InsightDraft(List.of());
                save(InsightAttemptStatus.VALID, empty, null);
                return empty;
            }
            InsightDraft draft;
            List<String> errors;
            try {
                draft = call(llm, InsightDraftRequest.first(input, contract), deadline);
                save(InsightAttemptStatus.DRAFTED, draft, null);
                errors = InsightDraftValidator.validate(draft, input.candidates(), contract);
            } catch (InsightLlmException ex) {
                if (ex.failure().kind() != LlmFailure.Kind.INVALID_OUTPUT) {
                    throw ex;
                }
                draft = new InsightDraft(List.of());
                errors = List.of("The previous answer could not be used: " + ex.getMessage());
            }
            if (errors.isEmpty()) {
                save(InsightAttemptStatus.VALID, draft, null);
                return draft;
            }
            save(InsightAttemptStatus.INVALID, draft, errors);
            log.info("Repairing insight draft jobId={} errors={}", job.id(), errors.size());
            InsightDraft repaired;
            try {
                repaired = call(llm, InsightDraftRequest.repair(input, contract, draft, errors), deadline);
            } catch (InsightLlmException ex) {
                if (ex.failure().kind() == LlmFailure.Kind.INVALID_OUTPUT) {
                    failInvalid(List.of(ex.getMessage()));
                    return null;
                }
                throw ex;
            }
            List<String> remaining = InsightDraftValidator.validate(repaired, input.candidates(), contract);
            if (!remaining.isEmpty()) {
                save(InsightAttemptStatus.INVALID, repaired, remaining);
                failInvalid(remaining);
                return null;
            }
            save(InsightAttemptStatus.VALID, repaired, null);
            return repaired;
        }

        private InsightDraft call(InsightsLlmGateway llm, InsightDraftRequest request, Deadline deadline) {
            int retries = 0;
            while (true) {
                if (deadline.expired()) {
                    throw new InsightLlmException(LlmFailure.of(LlmFailure.Kind.TIMEOUT),
                            "Insight job deadline of " + jobSettings.deadline() + " exhausted", null);
                }
                llmCalls++;
                try {
                    return llm.draft(request);
                } catch (InsightLlmException ex) {
                    LlmFailure failure = ex.failure();
                    if (!failure.retryable() || retries >= retryPolicy.maxRetries()) {
                        throw ex;
                    }
                    retries++;
                    Duration delay = retryPolicy.backoff(retries, failure, random.nextDouble());
                    if (delay.compareTo(deadline.remaining()) >= 0) {
                        throw ex;
                    }
                    log.warn("Insight draft call failed jobId={} kind={} status={} retry={} delayMs={}",
                            job.id(), failure.kind(), failure.httpStatus(), retries, delay.toMillis());
                    pause(delay, ex);
                }
            }
        }

        private void pause(Duration delay, InsightLlmException cause) {
            try {
                sleeper.sleep(delay);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw cause;
            }
        }

        private Outcome publish(InsightDraft draft, InsightGenerationInput input) {
            MarketInsightPublication publication = new MarketInsightPublication(
                    null,
                    job.id(),
                    attempt.id(),
                    job.snapshotId(),
                    input.bucketKey(),
                    input.comparisonBucketKey(),
                    InsightNarrativeRenderer.renderPayload(draft, input),
                    generationSettings.provider(),
                    generationSettings.model(),
                    job.promptVersion(),
                    now()
            );
            try {
                insightJobGateway.publish(publication, owner, now());
                return Outcome.PUBLISHED;
            } catch (InsightLeaseLostException ex) {
                log.warn("Insight job lease lost before publishing jobId={}", job.id());
                return Outcome.LEASE_LOST;
            } catch (RuntimeException ex) {
                log.warn("Insight publication failed jobId={} cause={}", job.id(), ex.toString());
                return retryLater("PUBLISH", ex.toString(), null);
            }
        }

        private Outcome llmFailure(InsightLlmException ex) {
            LlmFailure failure = ex.failure();
            String kind = failure.kind().name();
            recordAttemptFailure(kind, ex.getMessage());
            if (failure.retryable()) {
                return retryLater(kind, ex.getMessage(), failure.retryAfter());
            }
            return fail(kind, ex.getMessage());
        }

        private void failInvalid(List<String> errors) {
            terminal = fail(LlmFailure.Kind.INVALID_OUTPUT.name(),
                    "Draft still invalid after one repair: " + String.join("; ", errors));
        }

        private Outcome retryLater(String kind, String message, Duration retryAfter) {
            if (job.attemptCount() >= jobSettings.maxAttempts()) {
                return fail(kind, message + " (attempts exhausted)");
            }
            Duration wait = jobSettings.rescheduleDelay().multipliedBy(1L << Math.min(job.attemptCount() - 1, 10));
            if (retryAfter != null && retryAfter.compareTo(wait) > 0) {
                wait = retryAfter;
            }
            Instant now = now();
            return insightJobGateway.reschedule(job.id(), owner, now.plus(wait), error(kind, message), now)
                    ? Outcome.RESCHEDULED
                    : Outcome.LEASE_LOST;
        }

        private Outcome fail(String kind, String message) {
            return insightJobGateway.fail(job.id(), owner, error(kind, message), now())
                    ? Outcome.FAILED
                    : Outcome.LEASE_LOST;
        }

        private void recordAttemptFailure(String kind, String message) {
            if (attempt == null || attempt.status() == InsightAttemptStatus.VALID) {
                return;
            }
            attempt = insightJobGateway.saveAttempt(new InsightAttempt(attempt.id(), job.id(), attempt.attemptNumber(),
                    InsightAttemptStatus.FAILED, attempt.draft(), attempt.validationErrors(), kind,
                    truncate(message), llmCalls, attempt.startedAt(), now()));
        }

        private void save(InsightAttemptStatus status, InsightDraft draft, List<String> errors) {
            boolean finished = status == InsightAttemptStatus.VALID;
            attempt = insightJobGateway.saveAttempt(new InsightAttempt(attempt.id(), job.id(), attempt.attemptNumber(),
                    status, insightDraftCodec.encode(draft), errors == null ? null : truncate(String.join("\n", errors)),
                    status == InsightAttemptStatus.INVALID ? LlmFailure.Kind.INVALID_OUTPUT.name() : null, null,
                    llmCalls, attempt.startedAt(), finished ? now() : null));
        }
    }

    private Instant now() {
        return clock.instant();
    }

    private static String error(String kind, String message) {
        return truncate(kind + ": " + message);
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message;
    }
}
