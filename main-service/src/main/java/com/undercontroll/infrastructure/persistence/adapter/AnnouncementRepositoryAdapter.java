package com.undercontroll.infrastructure.persistence.adapter;

import com.undercontroll.domain.model.Announcement;
import com.undercontroll.domain.port.out.AnnouncementRepositoryPort;
import com.undercontroll.infrastructure.persistence.entity.AnnouncementJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AnnouncementRepositoryAdapter implements AnnouncementRepositoryPort {

    private final AnnouncementRepository announcementRepository;

    @Override
    public Announcement save(Announcement announcement) {
        AnnouncementJpaEntity jpaEntity = AnnouncementJpaEntity.fromDomain(announcement);
        AnnouncementJpaEntity savedEntity = announcementRepository.save(jpaEntity);
        return savedEntity.toDomain();
    }

    @Override
    public void deleteById(Integer id) {
        announcementRepository.deleteById(id);
    }

    @Override
    public Optional<Announcement> findById(Integer id) {
        return announcementRepository.findById(id).map(AnnouncementJpaEntity::toDomain);
    }

    @Override
    public List<Announcement> findAll() {
        return announcementRepository.findAll().stream()
                .map(AnnouncementJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Announcement> findAllPaginated(int page, int size) {
        return announcementRepository.findAllPaginated(PageRequest.of(page, size)).stream()
                .map(AnnouncementJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Announcement> findLastAnnouncement() {
        return announcementRepository.findLastAnnouncement()
                .map(AnnouncementJpaEntity::toDomain);
    }

}
