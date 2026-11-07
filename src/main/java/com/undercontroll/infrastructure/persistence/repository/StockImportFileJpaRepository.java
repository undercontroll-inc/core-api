package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.infrastructure.persistence.entity.StockImportFileJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;

@Repository
public interface StockImportFileJpaRepository extends JpaRepository<StockImportFileJpaEntity, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM StockImportFileJpaEntity f
            WHERE f.importId IN (
                SELECT i.id FROM StockImportJpaEntity i
                WHERE i.status IN :statuses AND i.updatedAt < :before
            )
            """)
    int purge(@Param("statuses") Collection<StockImportStatus> statuses, @Param("before") Instant before);
}
