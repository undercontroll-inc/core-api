package com.undercontroll.presentation.controller.impl;

import com.undercontroll.application.usecase.announcement.CreateAnnouncementPort;
import com.undercontroll.application.usecase.announcement.DeleteAnnouncementPort;
import com.undercontroll.application.usecase.announcement.GetAnnouncementsPort;
import com.undercontroll.application.usecase.announcement.GetLastAnnouncementPort;
import com.undercontroll.application.usecase.announcement.UpdateAnnouncementPort;
import com.undercontroll.application.dto.AnnouncementDto;
import com.undercontroll.presentation.controller.AnnouncementApi;
import com.undercontroll.presentation.dto.CreateAnnouncementRequest;
import com.undercontroll.presentation.dto.CreateAnnouncementResponse;
import com.undercontroll.presentation.dto.UpdateAnnouncementRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping(value = "/v1/api/announcements", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
public class AnnouncementController implements AnnouncementApi {

    private final CreateAnnouncementPort createAnnouncement;
    private final GetAnnouncementsPort getAnnouncements;
    private final UpdateAnnouncementPort updateAnnouncement;
    private final DeleteAnnouncementPort deleteAnnouncement;
    private final GetLastAnnouncementPort getLastAnnouncement;

    @Override
    @PostMapping
    public ResponseEntity<CreateAnnouncementResponse> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request,
            @RequestHeader("Authorization") String auth
    ) {
        String token = auth.split("Bearer ")[1];

        CreateAnnouncementPort.Output output = createAnnouncement.execute(
                new CreateAnnouncementPort.Input(request.title(), request.description(), token, request.type())
        );
        return ResponseEntity.status(201).body(
                new CreateAnnouncementResponse(
                        output.id(),
                        output.title(),
                        output.content(),
                        output.type(),
                        output.publishedAt(),
                        output.updatedAt()
                )
        );
    }

    @Override
    @GetMapping
    public ResponseEntity<List<AnnouncementDto>> getAllAnnouncements(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        List<AnnouncementDto> announcements = getAnnouncements
                .execute(new GetAnnouncementsPort.Input(page, size))
                .announcements();
        return announcements.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(announcements);
    }

    @Override
    @PutMapping(value = "/{announcementId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnnouncementDto> updateAnnouncement(
            @Valid @RequestBody UpdateAnnouncementRequest request,
            @PathVariable Integer announcementId
    ) {
        UpdateAnnouncementPort.Output output = updateAnnouncement.execute(
                new UpdateAnnouncementPort.Input(announcementId, request.title(), request.content(), request.type())
        );
        return ResponseEntity.ok(
                new AnnouncementDto(
                        output.id(),
                        output.title(),
                        output.content(),
                        output.type(),
                        output.publishedAt(),
                        output.updatedAt()
                )
        );
    }

    @Override
    @DeleteMapping("/{announcementId}")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Integer announcementId) {
        deleteAnnouncement.execute(new DeleteAnnouncementPort.Input(announcementId));
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping("/last")
    public ResponseEntity<AnnouncementDto> getLastAnnouncement() {
        return getLastAnnouncement.execute()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
