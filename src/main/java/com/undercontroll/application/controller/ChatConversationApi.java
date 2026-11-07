package com.undercontroll.application.controller;

import com.undercontroll.application.controller.doc.ApiResponseDocumentation.ErrorResponse;
import com.undercontroll.application.dto.chat.ConversationMessagesDto;
import com.undercontroll.application.dto.chat.ConversationPageDto;
import com.undercontroll.application.dto.chat.ConversationSummaryDto;
import com.undercontroll.application.dto.chat.RenameConversationRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;

@Tag(name = "Chat history", description = "Ana conversations of the signed-in user")
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping(value = "/v1/api/chats/conversations", produces = MediaType.APPLICATION_JSON_VALUE)
public interface ChatConversationApi {

    @Operation(summary = "List conversations, most recent activity first (keyset pagination)")
    @ApiResponse(responseCode = "200", description = "Page of conversations",
            content = @Content(schema = @Schema(implementation = ConversationPageDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid cursor or limit (code VALIDATION_ERROR)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Only ADMINISTRATOR",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    ResponseEntity<ConversationPageDto> list(
            @Parameter(description = "nextCursor from the previous page") @RequestParam(required = false) String cursor,
            @Parameter(description = "1 to 50, default 20") @RequestParam(required = false) Integer limit
    );

    @Operation(summary = "Get one conversation")
    @ApiResponse(responseCode = "200", description = "Conversation",
            content = @Content(schema = @Schema(implementation = ConversationSummaryDto.class)))
    @ApiResponse(responseCode = "404", description = "Not found or deleted (code CONVERSATION_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{conversationId}")
    ResponseEntity<ConversationSummaryDto> get(@PathVariable String conversationId);

    @Operation(
            summary = "Messages of a conversation",
            description = "Without cursor returns the latest page; nextCursor walks to older messages. Items are oldest first "
                    + "within the page. Supports If-None-Match (ETag) and answers 304 when nothing changed."
    )
    @ApiResponse(responseCode = "200", description = "Page of messages",
            content = @Content(schema = @Schema(implementation = ConversationMessagesDto.class)))
    @ApiResponse(responseCode = "304", description = "Not modified since the given ETag")
    @ApiResponse(responseCode = "400", description = "Invalid cursor or limit (code VALIDATION_ERROR)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found or deleted (code CONVERSATION_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{conversationId}/messages")
    ResponseEntity<ConversationMessagesDto> messages(
            @PathVariable String conversationId,
            @Parameter(description = "nextCursor from the previous page") @RequestParam(required = false) String cursor,
            @Parameter(description = "1 to 50, default 50") @RequestParam(required = false) Integer limit,
            @Parameter(hidden = true) WebRequest webRequest
    );

    @Operation(summary = "Rename a conversation")
    @ApiResponse(responseCode = "200", description = "Renamed",
            content = @Content(schema = @Schema(implementation = ConversationSummaryDto.class)))
    @ApiResponse(responseCode = "400", description = "Blank title or over 120 characters (code VALIDATION_ERROR)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Not found or deleted (code CONVERSATION_NOT_FOUND)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping(value = "/{conversationId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ConversationSummaryDto> rename(
            @PathVariable String conversationId,
            @RequestBody RenameConversationRequest request
    );

    @Operation(
            summary = "Delete a conversation",
            description = "Idempotent. The id cannot be reused afterwards: sending a message with it returns 404, so start a new conversationId."
    )
    @ApiResponse(responseCode = "204", description = "Deleted")
    @DeleteMapping("/{conversationId}")
    ResponseEntity<Void> delete(@PathVariable String conversationId);
}
