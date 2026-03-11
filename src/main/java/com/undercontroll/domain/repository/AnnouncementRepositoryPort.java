package com.undercontroll.domain.repository;

import com.undercontroll.domain.model.Announcement;

import java.util.List;
import java.util.Optional;

public interface AnnouncementRepositoryPort {

    Announcement save(Announcement announcement);

    void deleteById(Integer id);

    Optional<Announcement> findById(Integer id);

    List<Announcement> findAll();

    List<Announcement> findAllPaginated(int page, int size);

    Optional<Announcement> findLastAnnouncement();

}
