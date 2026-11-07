package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.exception.AnaUnavailableException;
import com.undercontroll.domain.exception.ConversationBusyException;
import com.undercontroll.domain.exception.ConversationMemoryUnavailableException;
import com.undercontroll.domain.gateway.AnaChatGateway;
import com.undercontroll.domain.gateway.AnaConversationMemory;
import com.undercontroll.domain.gateway.AnaConversationMemory.ConversationLock;
import com.undercontroll.domain.gateway.ConversationGateway;
import com.undercontroll.domain.gateway.ConversationListVersion;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.gateway.ShopSnapshotLoader;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.chat.AnaReply;
import com.undercontroll.domain.model.chat.AnaTurnRequest;
import com.undercontroll.domain.model.chat.AnaWebSearchNeed;
import com.undercontroll.domain.model.chat.AppendedMessage;
import com.undercontroll.domain.model.chat.ChatMessage;
import com.undercontroll.domain.model.chat.ConversationKey;
import com.undercontroll.domain.model.chat.ShopBriefingWriter;
import com.undercontroll.domain.model.chat.StoredChatMessage;
import com.undercontroll.domain.support.Deadline;
import com.undercontroll.domain.usecase.chat.AnaChatSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
class AnaTurnPreparer {

    private final ObjectProvider<AnaChatGateway> anaChatGateway;
    private final CurrentUserIdPort currentUserIdPort;
    private final ShopSnapshotLoader shopSnapshotLoader;
    private final OrderGateway orderGateway;
    private final AnaConversationMemory memory;
    private final ConversationGateway conversations;
    private final ConversationListVersion listVersion;
    private final Clock clock;
    private final Duration lockTtl;
    private final Duration turnTimeout;
    private final int historyWindow;

    AnaTurnPreparer(
            ObjectProvider<AnaChatGateway> anaChatGateway,
            CurrentUserIdPort currentUserIdPort,
            ShopSnapshotLoader shopSnapshotLoader,
            OrderGateway orderGateway,
            AnaConversationMemory memory,
            ConversationGateway conversations,
            ConversationListVersion listVersion,
            Clock clock,
            @Value("${undercontroll.ai.ana.lock-ttl:90s}") Duration lockTtl,
            @Value("${undercontroll.ai.ana.turn-timeout:60s}") Duration turnTimeout,
            @Value("${undercontroll.ai.ana.memory-window:10}") int historyWindow
    ) {
        this.anaChatGateway = anaChatGateway;
        this.currentUserIdPort = currentUserIdPort;
        this.shopSnapshotLoader = shopSnapshotLoader;
        this.orderGateway = orderGateway;
        this.memory = memory;
        this.conversations = conversations;
        this.listVersion = listVersion;
        this.clock = clock;
        this.lockTtl = lockTtl;
        this.turnTimeout = turnTimeout;
        this.historyWindow = Math.max(2, historyWindow);
    }

    AnaChatGateway gateway() {
        AnaChatGateway llm = anaChatGateway.getIfAvailable();
        if (llm == null) {
            throw new AnaUnavailableException();
        }
        return llm;
    }

    AnaChatSession prepare(String message, String conversationId) {
        Deadline deadline = Deadline.after(turnTimeout);
        gateway();
        ConversationKey key = ConversationKey.of(currentUserIdPort.require(), conversationId);
        ConversationLock lock = lock(key);
        try {
            AppendedMessage appended = conversations.appendUserMessage(key, message, clock.instant());
            listVersion.bump(key.userId());
            boolean memoryAvailable = lock != null;
            List<ChatMessage> history = List.of();
            if (memoryAvailable) {
                try {
                    history = memory.load(key);
                } catch (ConversationMemoryUnavailableException ex) {
                    log.warn("Ana memory read failed, using stored history conversationId={}", key.conversationId());
                    memoryAvailable = false;
                }
            }
            if (history.isEmpty() && appended.seq() > 1) {
                history = storedHistory(appended);
                if (memoryAvailable && !history.isEmpty()) {
                    seed(key, history);
                }
            }
            List<String> texts = history.stream().map(ChatMessage::text).toList();
            String briefing = ShopBriefingWriter.chatBriefing(shopSnapshotLoader.load());
            List<Order> cited = ShopBriefingWriter.mentionedOrderIds(scope(message, texts)).stream()
                    .map(orderGateway::findDetailById)
                    .flatMap(Optional::stream)
                    .toList();
            briefing = ShopBriefingWriter.appendOrderDetails(briefing, cited);
            boolean webSearch = AnaWebSearchNeed.matchesConversation(message, texts);
            AnaTurnRequest turn = new AnaTurnRequest(key.conversationId(), message, briefing, history, webSearch, deadline);
            return new AnaChatSession(key, turn, lock, memoryAvailable, appended.conversation().id(), appended.messageId());
        } catch (RuntimeException ex) {
            release(lock);
            throw ex;
        }
    }

    Long finish(AnaChatSession session, AnaReply reply) {
        Long assistantMessageId = null;
        try {
            assistantMessageId = store(session, reply);
            if (reply != null && reply.isCompleted() && session.memoryAvailable()) {
                memory.append(
                        session.key(),
                        ChatMessage.user(session.turn().message()),
                        ChatMessage.assistant(reply.content())
                );
            }
        } catch (ConversationMemoryUnavailableException ex) {
            log.warn("Ana memory write failed conversationId={}", session.conversationId());
        } finally {
            release(session.lock());
        }
        return assistantMessageId;
    }

    void abandon(AnaChatSession session) {
        release(session.lock());
    }

    private Long store(AnaChatSession session, AnaReply reply) {
        if (reply == null || session.conversationPk() == null || reply.content() == null || reply.content().isBlank()) {
            return null;
        }
        try {
            AppendedMessage stored = conversations.appendAssistantMessage(
                    session.conversationPk(), reply.content(), reply.state(), reply.reason(), clock.instant());
            listVersion.bump(session.key().userId());
            return stored.messageId();
        } catch (RuntimeException ex) {
            log.error("Ana reply could not be stored conversationId={}", session.conversationId(), ex);
            return null;
        }
    }

    private List<ChatMessage> storedHistory(AppendedMessage appended) {
        List<StoredChatMessage> recent = conversations.messagesBefore(
                appended.conversation().id(), appended.seq(), historyWindow * 2);
        List<ChatMessage> history = new ArrayList<>();
        for (int i = recent.size() - 1; i >= 0; i--) {
            StoredChatMessage message = recent.get(i);
            if (message.role() == ChatMessage.Role.ASSISTANT && message.completed()
                    && !history.isEmpty() && history.getLast().role() == ChatMessage.Role.USER) {
                history.add(ChatMessage.assistant(message.content()));
            } else if (message.role() == ChatMessage.Role.USER) {
                if (!history.isEmpty() && history.getLast().role() == ChatMessage.Role.USER) {
                    history.removeLast();
                }
                history.add(ChatMessage.user(message.content()));
            }
        }
        if (!history.isEmpty() && history.getLast().role() == ChatMessage.Role.USER) {
            history.removeLast();
        }
        int from = Math.max(0, history.size() - historyWindow);
        if (from % 2 == 1) {
            from++;
        }
        return List.copyOf(history.subList(from, history.size()));
    }

    private void seed(ConversationKey key, List<ChatMessage> history) {
        try {
            memory.seed(key, history);
        } catch (ConversationMemoryUnavailableException ex) {
            log.warn("Ana memory seed failed conversationId={}", key.conversationId());
        }
    }

    private ConversationLock lock(ConversationKey key) {
        try {
            return memory.tryLock(key, lockTtl).orElseThrow(ConversationBusyException::new);
        } catch (ConversationMemoryUnavailableException ex) {
            log.warn("Ana memory unavailable, answering in degraded mode conversationId={}", key.conversationId());
            return null;
        }
    }

    private static void release(ConversationLock lock) {
        if (lock == null) {
            return;
        }
        try {
            lock.release();
        } catch (RuntimeException ex) {
            log.warn("Ana conversation lock release failed cause={}", ex.toString());
        }
    }

    private static String scope(String current, List<String> history) {
        String message = current == null ? "" : current;
        if (history.isEmpty()) {
            return message;
        }
        return message + "\n" + String.join("\n", history);
    }
}
