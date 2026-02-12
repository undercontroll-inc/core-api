package com.undercontroll.controller.impl;

import com.undercontroll.controller.AnnouncementApi;
import com.undercontroll.dto.AnnouncementDto;
import com.undercontroll.dto.CreateAnnouncementRequest;
import com.undercontroll.dto.CreateAnnouncementResponse;
import com.undercontroll.dto.UpdateAnnouncementRequest;
import com.undercontroll.service.AnnouncementService;
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

    private final AnnouncementService service;

    @Override
    public ResponseEntity<CreateAnnouncementResponse> createAnnouncement(@Valid @RequestBody CreateAnnouncementRequest request) {
        var response = service.createAnnouncement(request);
        return ResponseEntity.status(201).body(response);
    }

    @Override
    @GetMapping
    public ResponseEntity<List<AnnouncementDto>> getAllAnnouncements(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer size) {
        var response = service.getAllAnnouncementsPaginated(page, size);
        return response.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }

    @Override
    @PutMapping(value = "/{announcementId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnnouncementDto> updateAnnouncement(@Valid @RequestBody UpdateAnnouncementRequest request, @PathVariable Integer announcementId) {
        AnnouncementDto response = service.updateAnnouncement(request, announcementId);
        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{announcementId}")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Integer announcementId) {
        service.deleteAnnouncement(announcementId);
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping("/last")
    public ResponseEntity<AnnouncementDto> getLastAnnouncement() {
        AnnouncementDto announcement = service.getLastAnnouncement();

        return announcement == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(announcement);
    }
}

