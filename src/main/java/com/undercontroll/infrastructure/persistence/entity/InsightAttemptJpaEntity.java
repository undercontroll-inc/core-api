package com.undercontroll.infrastructure.persistence.entity;

import com.undercontroll.domain.enums.InsightAttemptStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "insight_attempt",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_insight_attempt_number",
                columnNames = {"job_id", "attempt_number"}
        )
)
public class InsightAttemptJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InsightAttemptStatus status;

    @Column(columnDefinition = "TEXT")
    private String draft;

    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;

    @Column(name = "error_kind", length = 30)
    private String errorKind;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "llm_calls", nullable = false)
    private Integer llmCalls;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;
}
