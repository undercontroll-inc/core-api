package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.chat.AnaReply;
import com.undercontroll.domain.model.chat.AppendedMessage;
import com.undercontroll.domain.model.chat.ChatCursor;
import com.undercontroll.domain.model.chat.Conversation;
import com.undercontroll.domain.model.chat.ConversationKey;
import com.undercontroll.domain.model.chat.StoredChatMessage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConversationGateway {

    Optional<Conversation> findActive(Integer userId, String publicId);

    AppendedMessage appendUserMessage(ConversationKey key, String content, Instant now);

    AppendedMessage appendAssistantMessage(Long conversationId, String content, AnaReply.State state, String reason, Instant now);

    List<Conversation> list(Integer userId, ChatCursor.ConversationPosition after, int limit);

    List<StoredChatMessage> messagesBefore(Long conversationId, Integer beforeSeq, int limit);

    Optional<Conversation> rename(Integer userId, String publicId, String title, Instant now);

    boolean softDelete(Integer userId, String publicId, Instant now);

    int purgeDeleted(Instant deletedBefore);
}
