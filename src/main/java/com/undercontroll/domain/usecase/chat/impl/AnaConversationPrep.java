package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.exception.AnaUnavailableException;
import com.undercontroll.domain.gateway.AnaChatGateway;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.chat.ShopSuggestionComposer;
import com.undercontroll.domain.usecase.chat.StreamChatSession;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

final class AnaConversationPrep {

    private AnaConversationPrep() {
    }

    static Prepared prepare(
            ObjectProvider<AnaChatGateway> anaChatGateway,
            CurrentUserIdPort currentUserIdPort,
            ShopSnapshotLoader shopSnapshotLoader,
            OrderGateway orderGateway,
            String message
    ) {
        AnaChatGateway llm = anaChatGateway.getIfAvailable();
        if (llm == null) {
            throw new AnaUnavailableException();
        }
        Integer userId = currentUserIdPort.require();
        String conversationId = String.valueOf(userId);
        String briefing = ShopSuggestionComposer.chatBriefing(shopSnapshotLoader.load());
        String scope = conversationScope(message, llm.recentConversationTexts(conversationId));
        List<Order> cited = ShopSuggestionComposer.mentionedOrderIds(scope).stream()
                .map(orderGateway::findDetailById)
                .flatMap(Optional::stream)
                .toList();
        briefing = ShopSuggestionComposer.appendOrderDetails(briefing, cited);
        return new Prepared(llm, userId, conversationId, briefing);
    }

    private static String conversationScope(String current, List<String> history) {
        String message = current == null ? "" : current;
        if (history == null || history.isEmpty()) {
            return message;
        }
        return message + "\n" + String.join("\n", history);
    }

    record Prepared(AnaChatGateway llm, Integer userId, String conversationId, String briefing) {

        StreamChatSession toSession(String message) {
            return new StreamChatSession(conversationId, message, briefing);
        }
    }
}
