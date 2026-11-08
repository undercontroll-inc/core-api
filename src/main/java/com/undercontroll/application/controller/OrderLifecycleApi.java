package com.undercontroll.application.controller;

import com.undercontroll.application.controller.doc.ApiResponseDocumentation.ErrorResponse;
import com.undercontroll.application.dto.order.BudgetDecisionRequest;
import com.undercontroll.application.dto.order.LabelReadingResponse;
import com.undercontroll.application.dto.order.OrderEnrichedDto;
import com.undercontroll.application.dto.order.OrderLookupResponse;
import com.undercontroll.application.dto.order.OrderPhotoDto;
import com.undercontroll.application.dto.order.PhotoUploadRequest;
import com.undercontroll.application.dto.storage.UploadReference;
import com.undercontroll.application.dto.storage.UploadRequest;
import com.undercontroll.application.dto.storage.UploadTicketDto;
import com.undercontroll.application.dto.order.SendBudgetResponse;
import com.undercontroll.application.dto.order.TrackingLinkResponse;
import com.undercontroll.application.dto.order.TransitionOrderRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Tag(name = "Order lifecycle", description = "Status transitions, budget approval, tracking link, photos and label reading")
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping(value = "/v1/api/orders", produces = MediaType.APPLICATION_JSON_VALUE)
public interface OrderLifecycleApi {

    @Operation(summary = "Move an order to another status",
            description = "RECEIVED→AWAITING_APPROVAL|READY_FOR_PICKUP; AWAITING_APPROVAL→IN_REPAIR|READY_FOR_PICKUP|RECEIVED; "
                    + "IN_REPAIR→READY_FOR_PICKUP; READY_FOR_PICKUP→DELIVERED.")
    @ApiResponse(responseCode = "200", description = "Order after the transition")
    @ApiResponse(responseCode = "400", description = "Empty budget (code ORDER_BUDGET_EMPTY)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Invalid transition (ORDER_INVALID_TRANSITION) or concurrent change (ORDER_CONCURRENT_UPDATE)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/{orderId}/transitions", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<OrderEnrichedDto> transition(@PathVariable Integer orderId, @RequestBody TransitionOrderRequest request);

    @Operation(summary = "Send the budget: snapshot the values, move to AWAITING_APPROVAL and get the WhatsApp text")
    @ApiResponse(responseCode = "200", description = "Tracking link, text and order")
    @ApiResponse(responseCode = "400", description = "Budget total is zero (code ORDER_BUDGET_EMPTY)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Order is not RECEIVED (code ORDER_INVALID_TRANSITION)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{orderId}/budget/send")
    ResponseEntity<SendBudgetResponse> sendBudget(@PathVariable Integer orderId);

    @Operation(summary = "Register the customer's answer received by phone or WhatsApp (idempotent)")
    @ApiResponse(responseCode = "200", description = "Order after the decision")
    @ApiResponse(responseCode = "409", description = "Not awaiting approval or already decided differently (code ORDER_INVALID_TRANSITION)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/{orderId}/budget/decision", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<OrderEnrichedDto> decideBudget(@PathVariable Integer orderId, @RequestBody BudgetDecisionRequest request);

    @Operation(summary = "Tracking link and WhatsApp text for the current status (creates the link if missing)")
    @PostMapping("/{orderId}/tracking-link")
    ResponseEntity<TrackingLinkResponse> trackingLink(@PathVariable Integer orderId);

    @Operation(summary = "Find the order of a scanned tracking QR code")
    @ApiResponse(responseCode = "404", description = "No order for this token (code ORDER_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/lookup")
    ResponseEntity<OrderLookupResponse> lookup(@RequestParam String token);

    @Operation(summary = "Get a presigned URL to upload a label photo (step 1 of label reading)")
    @ApiResponse(responseCode = "201", description = "Upload ticket",
            content = @Content(schema = @Schema(implementation = UploadTicketDto.class)))
    @ApiResponse(responseCode = "400", description = "Type not JPEG/PNG/WEBP or over 10MB (code LABEL_READING_INVALID)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "Label reading disabled (code LABEL_READING_UNAVAILABLE)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/label-reading/uploads", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<UploadTicketDto> requestLabelUpload(@RequestBody UploadRequest request);

    @Operation(summary = "Read the uploaded label with AI (step 2: after the PUT)")
    @ApiResponse(responseCode = "400", description = "Upload missing, expired or content does not match (code LABEL_READING_INVALID)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "Label reading disabled or failed (code LABEL_READING_UNAVAILABLE)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/label-reading", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<LabelReadingResponse> readLabel(@RequestBody UploadReference request);

    @Operation(summary = "Get a presigned URL to upload an order photo (max 6 per order)")
    @ApiResponse(responseCode = "201", description = "Upload ticket",
            content = @Content(schema = @Schema(implementation = UploadTicketDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid kind, type or size (code ORDER_PHOTO_INVALID / UPLOAD_INVALID)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Order already has 6 photos, counting pending uploads (code ORDER_PHOTO_LIMIT)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(value = "/{orderId}/photos/uploads", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<UploadTicketDto> requestPhotoUpload(@PathVariable Integer orderId, @RequestBody PhotoUploadRequest request);

    @Operation(summary = "Confirm an order photo after the PUT (idempotent)")
    @ApiResponse(responseCode = "201", description = "Photo stored")
    @ApiResponse(responseCode = "400", description = "Not uploaded yet, expired, size or content mismatch (code UPLOAD_INVALID)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/{orderId}/photos/uploads/{uploadId}/complete")
    ResponseEntity<OrderPhotoDto> completePhotoUpload(@PathVariable Integer orderId, @PathVariable UUID uploadId);

    @Operation(summary = "List the order photos (URLs valid for 15 minutes)")
    @GetMapping("/{orderId}/photos")
    ResponseEntity<List<OrderPhotoDto>> listPhotos(@PathVariable Integer orderId);

    @Operation(summary = "Delete an order photo")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @DeleteMapping("/{orderId}/photos/{photoId}")
    ResponseEntity<Void> deletePhoto(@PathVariable Integer orderId, @PathVariable Long photoId);
}
