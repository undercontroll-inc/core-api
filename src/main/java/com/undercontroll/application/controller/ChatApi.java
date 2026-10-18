package com.undercontroll.application.controller;

import com.undercontroll.application.dto.chat.ChatSuggestionsResponse;
import com.undercontroll.application.dto.chat.SendChatMessageRequest;
import com.undercontroll.application.dto.chat.SendChatMessageResponse;
import com.undercontroll.application.controller.doc.ApiResponseDocumentation.ErrorResponse;
import com.undercontroll.application.controller.doc.ApiResponseDocumentation.GetApiResponses;
import com.undercontroll.application.controller.doc.ApiResponseDocumentation.PostApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Chat", description = "Ana AI, the workshop assistant")
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping(value = "/v1/api/chats", produces = MediaType.APPLICATION_JSON_VALUE)
public interface ChatApi {

    @Operation(summary = "Send a question to Ana", description = "state is completed or partial (reason tells why, e.g. max_tokens)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reply with conversationId, content, state and reason"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request (blank content or invalid conversationId)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Another message of this conversation is still being answered (code CHAT_CONVERSATION_BUSY)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Ana is unavailable",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/messages")
    ResponseEntity<SendChatMessageResponse> sendMessage(@Valid @RequestBody SendChatMessageRequest message);

    @Operation(summary = "Send a question to Ana and stream the reply as SSE")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "SSE stream: start, status, delta events, then exactly one terminal event: "
                            + "done {state=completed, content}, partial {state=partial, content, reason, retryable} "
                            + "(max_tokens, interrupted, timeout) or error {state=failed, code CHAT_UNAVAILABLE|CHAT_TIMEOUT, message, retryable}. "
                            + "A client disconnect cancels generation and nothing is stored.",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Another message of this conversation is still being answered (code CHAT_CONVERSATION_BUSY)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "Ana is unavailable",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping(value = "/messages/stream", produces = {
            MediaType.TEXT_EVENT_STREAM_VALUE,
            MediaType.APPLICATION_JSON_VALUE
    })
    ResponseEntity<SseEmitter> streamMessage(@Valid @RequestBody SendChatMessageRequest message);

    @Operation(summary = "Get quick suggestions (generates and stores them in Redis if they do not exist yet)")
    @GetApiResponses
    @GetMapping("/suggestions")
    ResponseEntity<ChatSuggestionsResponse> getSuggestions();

    @Operation(summary = "Regenerate quick suggestions")
    @PostApiResponses
    @PostMapping("/suggestions")
    ResponseEntity<ChatSuggestionsResponse> refreshSuggestions();
}
