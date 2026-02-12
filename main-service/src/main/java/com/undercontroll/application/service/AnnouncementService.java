package com.undercontroll.application.service;

import com.undercontroll.infrastructure.web.dto.AnnouncementDto;
import com.undercontroll.infrastructure.web.dto.CreateAnnouncementRequest;
import com.undercontroll.infrastructure.web.dto.CreateAnnouncementResponse;
import com.undercontroll.infrastructure.web.dto.UpdateAnnouncementRequest;
import com.undercontroll.infrastructure.messaging.event.AnnouncementCreatedEvent;
import com.undercontroll.domain.exception.AnnouncementNotFoundException;
import com.undercontroll.domain.entity.Announcement;
import com.undercontroll.infrastructure.persistence.repository.AnnouncementRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final ApplicationEventPublisher publisher;
    private final MetricsService metricsService;

    @CacheEvict(value = {"announcements", "lastAnnouncement"}, allEntries = true)
    public CreateAnnouncementResponse createAnnouncement(
            @Valid CreateAnnouncementRequest request
    ) {
        Announcement announcement = Announcement.builder()
                .title(request.title())
                .content(request.description())
                .type(request.type())
                .build();

        Announcement announcementCreated = announcementRepository.save(announcement);

        // publica o evento que dispara email para usuarios que possuem um email cadastrado
        // para alertar sobre o novo anuncio
        publisher.publishEvent(new AnnouncementCreatedEvent(this,announcementCreated));

        metricsService.incrementAnnouncementCreated();

        log.info("New announcement event published successfully");

        return new CreateAnnouncementResponse(
                announcementCreated.getId(),
                announcementCreated.getTitle(),
                announcementCreated.getContent(),
                announcementCreated.getType(),
                announcementCreated.getPublishedAt(),
                announcementCreated.getUpdatedAt()
        );
    }

    @CacheEvict(value = {"announcements", "lastAnnouncement"}, allEntries = true)
    public AnnouncementDto updateAnnouncement(
            UpdateAnnouncementRequest request,
            Integer id
    ) {
        Announcement announcement =
                announcementRepository
                        .findById(id)
                        .orElseThrow(() -> new AnnouncementNotFoundException(
                                "Announcement with id " + id + " not found"
                        ));

        if(announcement.getTitle() != null){
            announcement.setTitle(request.title());
        }

        if(announcement.getContent() != null){
            announcement.setContent(request.content());
        }

        if(announcement.getType() != null){
            announcement.setType(request.type());
        }

        return this.mapToDto(announcementRepository.save(announcement));
    }

    @CacheEvict(value = {"announcements", "lastAnnouncement"}, allEntries = true)
    public void deleteAnnouncement(Integer id){
        Announcement announcement =
                announcementRepository
                        .findById(id)
                        .orElseThrow(() -> new AnnouncementNotFoundException(
                                "Announcement with id " + id + " not found"
                        ));

        announcementRepository.delete(announcement);
    }

    @Cacheable(value = "announcements", key = "#page + '-' + #size")
    public List<AnnouncementDto> getAllAnnouncementsPaginated(
            Integer page,
            Integer size
    ){
        List<Announcement> announcements = announcementRepository
                .findAllPaginated(PageRequest.of(page, size));

        return announcements.stream()
                .map(this::mapToDto)
                .toList();
    }

    @Cacheable(value = "lastAnnouncement")
    public AnnouncementDto getLastAnnouncement(){
        Optional<Announcement> announcement = announcementRepository.findLastAnnouncement();

        return announcement.map(this::mapToDto).orElse(null);
    }

    public AnnouncementDto mapToDto(Announcement announcement){
        return new AnnouncementDto(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getType(),
                announcement.getPublishedAt(),
                announcement.getUpdatedAt()
        );
    }

}
