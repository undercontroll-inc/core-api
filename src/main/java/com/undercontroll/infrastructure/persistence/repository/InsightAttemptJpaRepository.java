package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.infrastructure.persistence.entity.InsightAttemptJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InsightAttemptJpaRepository extends JpaRepository<InsightAttemptJpaEntity, Long> {

    Optional<InsightAttemptJpaEntity> findFirstByJobIdOrderByAttemptNumberDesc(Long jobId);
}
