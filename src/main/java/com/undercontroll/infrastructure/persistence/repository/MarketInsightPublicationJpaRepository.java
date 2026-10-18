package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.infrastructure.persistence.entity.MarketInsightPublicationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MarketInsightPublicationJpaRepository extends JpaRepository<MarketInsightPublicationJpaEntity, Long> {

    Optional<MarketInsightPublicationJpaEntity> findFirstByOrderByPublishedAtDescIdDesc();
}
