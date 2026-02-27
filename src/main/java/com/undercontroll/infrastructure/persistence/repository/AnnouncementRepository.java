package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.infrastructure.persistence.entity.AnnouncementJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnnouncementRepository extends JpaRepository<AnnouncementJpaEntity, Integer> {

    @Query("SELECT a FROM AnnouncementJpaEntity a")
    List<AnnouncementJpaEntity> findAllPaginated(Pageable pageable);

    @Query("SELECT a FROM AnnouncementJpaEntity a ORDER BY a.publishedAt DESC LIMIT 1")
    Optional<AnnouncementJpaEntity> findLastAnnouncement();
}
