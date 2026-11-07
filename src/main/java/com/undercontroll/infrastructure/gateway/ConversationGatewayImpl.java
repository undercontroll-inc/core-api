package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.exception.ConversationNotFoundException;
import com.undercontroll.domain.gateway.ConversationGateway;
import com.undercontroll.domain.model.chat.AnaReply;
import com.undercontroll.domain.model.chat.AppendedMessage;
import com.undercontroll.domain.model.chat.ChatCursor;
import com.undercontroll.domain.model.chat.ChatMessage;
import com.undercontroll.domain.model.chat.Conversation;
import com.undercontroll.domain.model.chat.ConversationKey;
import com.undercontroll.domain.model.chat.ConversationTitles;
import com.undercontroll.domain.model.chat.StoredChatMessage;
import com.undercontroll.infrastructure.mapper.ChatPersistenceMapper;
import com.undercontroll.infrastructure.persistence.entity.ChatConversationJpaEntity;
import com.undercontroll.infrastructure.persistence.entity.ChatMessageJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.ChatConversationJpaRepository;
import com.undercontroll.infrastructure.persistence.repository.ChatMessageJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class ConversationGatewayImpl implements ConversationGateway {

    static final int PURGE_BATCH = 200;

    private final ChatConversationJpaRepository conversations;
    private final ChatMessageJpaRepository messages;
    private final ChatPersistenceMapper mapper;
    private final TransactionTemplate newTransaction;

    public ConversationGatewayImpl(
            ChatConversationJpaRepository conversations,
            ChatMessageJpaRepository messages,
            ChatPersistenceMapper mapper,
            PlatformTransactionManager transactionManager
    ) {
        this.conversations = conversations;
        this.messages = messages;
        this.mapper = mapper;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversation> findActive(Integer userId, String publicId) {
        return conversations.findByUserIdAndPublicIdAndDeletedAtIsNull(userId, publicId).map(mapper::toDomain);
    }

    @Override
    public AppendedMessage appendUserMessage(ConversationKey key, String content, Instant now) {
        AppendedMessage appended = newTransaction.execute(status -> appendToExisting(key, content, now));
        if (appended != null) {
            return appended;
        }
        createIfAbsent(key, content, now);
        AppendedMessage retried = newTransaction.execute(status -> appendToExisting(key, content, now));
        if (retried == null) {
            throw new IllegalStateException("Conversation " + key.conversationId() + " could not be created");
        }
        return retried;
    }

    @Override
    @Transactional
    public AppendedMessage appendAssistantMessage(
            Long conversationId,
            String content,
            AnaReply.State state,
            String reason,
            Instant now
    ) {
        ChatConversationJpaEntity conversation = conversations.findByIdForUpdate(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(String.valueOf(conversationId)));
        return append(conversation, ChatMessage.Role.ASSISTANT, content, state, reason, now);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> list(Integer userId, ChatCursor.ConversationPosition after, int limit) {
        PageRequest page = PageRequest.of(0, limit);
        List<ChatConversationJpaEntity> found = after == null
                ? conversations.findRecent(userId, page)
                : conversations.findRecentAfter(userId, after.lastMessageAt(), after.id(), page);
        return found.stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoredChatMessage> messagesBefore(Long conversationId, Integer beforeSeq, int limit) {
        PageRequest page = PageRequest.of(0, limit);
        List<ChatMessageJpaEntity> found = beforeSeq == null
                ? messages.findLatest(conversationId, page)
                : messages.findBefore(conversationId, beforeSeq, page);
        return found.stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Optional<Conversation> rename(Integer userId, String publicId, String title, Instant now) {
        return conversations.findForUpdate(userId, publicId)
                .filter(conversation -> conversation.getDeletedAt() == null)
                .map(conversation -> {
                    conversation.setTitle(title);
                    conversation.setTitleSource(Conversation.TitleSource.USER);
                    conversation.setUpdatedAt(now);
                    return mapper.toDomain(conversations.saveAndFlush(conversation));
                });
    }

    @Override
    @Transactional
    public boolean softDelete(Integer userId, String publicId, Instant now) {
        return conversations.softDelete(userId, publicId, now) > 0;
    }

    @Override
    @Transactional
    public int purgeDeleted(Instant deletedBefore) {
        List<Long> ids = conversations.findDeletedBefore(deletedBefore, PageRequest.of(0, PURGE_BATCH));
        if (ids.isEmpty()) {
            return 0;
        }
        messages.deleteByConversationIds(ids);
        return conversations.deleteByIds(ids);
    }

    private AppendedMessage appendToExisting(ConversationKey key, String content, Instant now) {
        Optional<ChatConversationJpaEntity> locked = conversations.findForUpdate(key.userId(), key.conversationId());
        if (locked.isEmpty()) {
            return null;
        }
        ChatConversationJpaEntity conversation = locked.get();
        if (conversation.getDeletedAt() != null) {
            throw new ConversationNotFoundException(key.conversationId());
        }
        if (conversation.getMessageCount() == 0 && conversation.getTitleSource() == Conversation.TitleSource.AUTO) {
            conversation.setTitle(ConversationTitles.title(content));
        }
        return append(conversation, ChatMessage.Role.USER, content, AnaReply.State.COMPLETED, null, now);
    }

    private void createIfAbsent(ConversationKey key, String content, Instant now) {
        try {
            newTransaction.executeWithoutResult(status -> conversations.saveAndFlush(ChatConversationJpaEntity.builder()
                    .userId(key.userId())
                    .publicId(key.conversationId())
                    .title(ConversationTitles.title(content))
                    .titleSource(Conversation.TitleSource.AUTO)
                    .messageCount(0)
                    .lastSeq(0)
                    .createdAt(now)
                    .updatedAt(now)
                    .lastMessageAt(now)
                    .build()));
        } catch (DataIntegrityViolationException alreadyCreated) {
            return;
        }
    }

    private AppendedMessage append(
            ChatConversationJpaEntity conversation,
            ChatMessage.Role role,
            String content,
            AnaReply.State state,
            String reason,
            Instant now
    ) {
        int seq = conversation.getLastSeq() + 1;
        ChatMessageJpaEntity saved = messages.save(ChatMessageJpaEntity.builder()
                .conversationId(conversation.getId())
                .seq(seq)
                .role(role)
                .content(content)
                .state(state)
                .reason(reason == null ? null : reason.length() > 40 ? reason.substring(0, 40) : reason)
                .createdAt(now)
                .build());
        conversation.setLastSeq(seq);
        conversation.setMessageCount(conversation.getMessageCount() + 1);
        conversation.setLastMessagePreview(ConversationTitles.preview(content));
        conversation.setLastMessageAt(now);
        conversation.setUpdatedAt(now);
        ChatConversationJpaEntity updated = conversations.saveAndFlush(conversation);
        return new AppendedMessage(mapper.toDomain(updated), saved.getId(), seq);
    }
}
