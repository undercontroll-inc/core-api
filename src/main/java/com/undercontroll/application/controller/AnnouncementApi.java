package com.undercontroll.application.controller;

import com.undercontroll.application.controller.doc.ApiResponseDocumentation.*;
import com.undercontroll.application.dto.announcement.AnnouncementDto;
import com.undercontroll.application.dto.announcement.CreateAnnouncementRequest;
import com.undercontroll.application.dto.announcement.CreateAnnouncementResponse;
import com.undercontroll.application.dto.announcement.GetPaginatedAnnouncementResponse;
import com.undercontroll.application.dto.announcement.UpdateAnnouncementRequest;
import com.undercontroll.application.dto.announcement.UpdateAnnouncementResponse;
import com.undercontroll.domain.enums.AnnouncementType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Announcements", description = "APIs for managing announcements and notices")
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/v1/api/announcements")
public interface AnnouncementApi {

    @Operation(
            summary = "Create a new announcement",
            description = "Creates the announcement and, when imageUpload is sent, returns a presigned URL in imageUpload so the frontend can upload the image directly to S3. The backend persists only the image key."
    )
    @PostApiResponses
    @PostMapping
    ResponseEntity<CreateAnnouncementResponse> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request,
            @RequestHeader("Authorization") String authHeader
    );

    @Operation(
            summary = "List announcements (paginated)",
            description = "Lists announcements and returns a signed imageUrl for reading when the announcement has an image."
    )
    @GetApiResponses
    @GetMapping
    ResponseEntity<GetPaginatedAnnouncementResponse> getAllAnnouncements(
            @RequestParam(defaultValue = "0") @Parameter(example = "0") Integer page,
            @RequestParam(defaultValue = "10") @Parameter(example = "10") Integer size,
            @RequestParam(required = false) @Parameter(description = "Filter by announcement type") AnnouncementType type
    );

    @Operation(
            summary = "Update an announcement",
            description = "Updates announcement data. Send imageUpload to replace the image or removeImage=true to remove the current image; do not send both in the same request."
    )
    @PutApiResponses
    @PutMapping("/{announcementId}")
    ResponseEntity<UpdateAnnouncementResponse> updateAnnouncement(
            @Valid @RequestBody UpdateAnnouncementRequest request,
            @PathVariable @Parameter(example = "1") Integer announcementId
    );

    @Operation(summary = "Delete an announcement")
    @DeleteApiResponses
    @DeleteMapping("/{announcementId}")
    ResponseEntity<Void> deleteAnnouncement(@PathVariable @Parameter(example = "1") Integer announcementId);

    @Operation(
            summary = "Get the latest announcement",
            description = "Returns the most recent announcement and a signed imageUrl for reading when an image is registered."
    )
    @GetApiResponses
    @GetMapping("/latest")
    ResponseEntity<AnnouncementDto> getLatestAnnouncement();
}
