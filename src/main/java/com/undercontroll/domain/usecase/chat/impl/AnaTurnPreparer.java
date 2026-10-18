package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.exception.AnaUnavailableException;
import com.undercontroll.domain.exception.ConversationBusyException;
import com.undercontroll.domain.exception.ConversationMemoryUnavailableException;
import com.undercontroll.domain.gateway.AnaChatGateway;
import com.undercontroll.domain.gateway.AnaConversationMemory;
import com.undercontroll.domain.gateway.AnaConversationMemory.ConversationLock;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.gateway.ShopSnapshotLoader;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.chat.AnaReply;
import com.undercontroll.domain.model.chat.AnaTurnRequest;
import com.undercontroll.domain.model.chat.AnaWebSearchNeed;
import com.undercontroll.domain.model.chat.ChatMessage;
import com.undercontroll.domain.model.chat.ConversationKey;
import com.undercontroll.domain.model.chat.ShopBriefingWriter;
import com.undercontroll.domain.support.Deadline;
import com.undercontroll.domain.usecase.chat.AnaChatSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
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
    private final Duration lockTtl;
    private final Duration turnTimeout;

    AnaTurnPreparer(
            ObjectProvider<AnaChatGateway> anaChatGateway,
            CurrentUserIdPort currentUserIdPort,
            ShopSnapshotLoader shopSnapshotLoader,
            OrderGateway orderGateway,
            AnaConversationMemory memory,
            @Value("${undercontroll.ai.ana.lock-ttl:90s}") Duration lockTtl,
            @Value("${undercontroll.ai.ana.turn-timeout:60s}") Duration turnTimeout
    ) {
        this.anaChatGateway = anaChatGateway;
        this.currentUserIdPort = currentUserIdPort;
        this.shopSnapshotLoader = shopSnapshotLoader;
        this.orderGateway = orderGateway;
        this.memory = memory;
        this.lockTtl = lockTtl;
        this.turnTimeout = turnTimeout;
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
            boolean memoryAvailable = lock != null;
            List<ChatMessage> history = List.of();
            if (memoryAvailable) {
                try {
                    history = memory.load(key);
                } catch (ConversationMemoryUnavailableException ex) {
                    log.warn("Ana memory read failed, answering without history conversationId={}", key.conversationId());
                    memoryAvailable = false;
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
            return new AnaChatSession(key, turn, lock, memoryAvailable);
        } catch (RuntimeException ex) {
            release(lock);
            throw ex;
        }
    }

    void finish(AnaChatSession session, AnaReply reply) {
        try {
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
    }

    void abandon(AnaChatSession session) {
        release(session.lock());
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
