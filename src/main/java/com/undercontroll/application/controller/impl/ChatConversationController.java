package com.undercontroll.application.controller.impl;

import com.undercontroll.application.controller.ChatConversationApi;
import com.undercontroll.application.dto.chat.ConversationMessagesDto;
import com.undercontroll.application.dto.chat.ConversationPageDto;
import com.undercontroll.application.dto.chat.ConversationSummaryDto;
import com.undercontroll.application.dto.chat.RenameConversationRequest;
import com.undercontroll.application.mapper.ChatConversationDtoMapper;
import com.undercontroll.domain.usecase.chat.DeleteConversationPort;
import com.undercontroll.domain.usecase.chat.GetConversationPort;
import com.undercontroll.domain.usecase.chat.ListConversationMessagesPort;
import com.undercontroll.domain.usecase.chat.ListConversationsPort;
import com.undercontroll.domain.usecase.chat.RenameConversationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

@RestController
@RequiredArgsConstructor
public class ChatConversationController implements ChatConversationApi {

    private final ListConversationsPort listConversationsPort;
    private final GetConversationPort getConversationPort;
    private final ListConversationMessagesPort listConversationMessagesPort;
    private final RenameConversationPort renameConversationPort;
    private final DeleteConversationPort deleteConversationPort;
    private final ChatConversationDtoMapper mapper;

    @Override
    public ResponseEntity<ConversationPageDto> list(String cursor, Integer limit) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(mapper.toDto(listConversationsPort.execute(new ListConversationsPort.Input(cursor, limit))));
    }

    @Override
    public ResponseEntity<ConversationSummaryDto> get(String conversationId) {
        return ResponseEntity.ok(mapper.toDto(getConversationPort.execute(conversationId)));
    }

    @Override
    public ResponseEntity<ConversationMessagesDto> messages(
            String conversationId,
            String cursor,
            Integer limit,
            WebRequest webRequest
    ) {
        ListConversationMessagesPort.Output output = listConversationMessagesPort.execute(
                new ListConversationMessagesPort.Input(conversationId, cursor, limit));
        String etag = "\"" + output.version() + "\"";
        CacheControl cacheControl = CacheControl.noCache().cachePrivate();
        if (webRequest.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).cacheControl(cacheControl).build();
        }
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(cacheControl)
                .body(mapper.toDto(output.conversation().publicId(), output.page()));
    }

    @Override
    public ResponseEntity<ConversationSummaryDto> rename(String conversationId, RenameConversationRequest request) {
        String title = request == null ? null : request.title();
        return ResponseEntity.ok(mapper.toDto(renameConversationPort.execute(
                new RenameConversationPort.Input(conversationId, title))));
    }

    @Override
    public ResponseEntity<Void> delete(String conversationId) {
        deleteConversationPort.execute(conversationId);
        return ResponseEntity.noContent().build();
    }
}
