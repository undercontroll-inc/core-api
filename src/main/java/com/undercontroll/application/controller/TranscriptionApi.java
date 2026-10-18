package com.undercontroll.application.controller;

import com.undercontroll.application.dto.transcription.TranscribeAudioResponse;
import com.undercontroll.application.controller.doc.ApiResponseDocumentation.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Transcriptions", description = "Speech-to-text for workshop voice notes")
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping(value = "/v1/api/transcriptions")
public interface TranscriptionApi {

    @Operation(summary = "Transcribe a short voice note to text")
    @ApiResponse(
            responseCode = "200",
            description = "Transcription completed",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = TranscribeAudioResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Missing, empty, too large, or unsupported audio",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "Forbidden",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "503",
            description = "Speech-to-text is unavailable",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class)
            )
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TranscribeAudioResponse> transcribe(
            @RequestBody(description = "Short voice note", required = true)
            @RequestPart("audio") MultipartFile audio
    );
}
