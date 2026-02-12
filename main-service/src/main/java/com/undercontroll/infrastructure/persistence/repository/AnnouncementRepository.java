package com.undercontroll.infrastructure.persistence.repository;

import com.undercontroll.domain.entity.Announcement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Integer> {

    @Query("SELECT a FROM Announcement a")
    List<Announcement> findAllPaginated(Pageable pageable);

    @Query("SELECT a FROM Announcement a ORDER BY a.publishedAt DESC LIMIT 1")
    Optional<Announcement> findLastAnnouncement();
}
