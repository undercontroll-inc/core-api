package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.exception.AnaUnavailableException;
import com.undercontroll.domain.gateway.AnaChatGateway;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.gateway.ShopSnapshotLoader;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.chat.AnaTurnRequest;
import com.undercontroll.domain.model.chat.AnaWebSearchNeed;
import com.undercontroll.domain.model.chat.ShopBriefingWriter;
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
        String briefing = ShopBriefingWriter.chatBriefing(shopSnapshotLoader.load());
        List<String> history = llm.recentConversationTexts(conversationId);
        List<Order> cited = ShopBriefingWriter.mentionedOrderIds(conversationScope(message, history)).stream()
                .map(orderGateway::findDetailById)
                .flatMap(Optional::stream)
                .toList();
        briefing = ShopBriefingWriter.appendOrderDetails(briefing, cited);
        boolean webSearch = AnaWebSearchNeed.matchesConversation(message, history);
        return new Prepared(llm, userId, new AnaTurnRequest(conversationId, message, briefing, webSearch));
    }

    private static String conversationScope(String current, List<String> history) {
        String message = current == null ? "" : current;
        if (history == null || history.isEmpty()) {
            return message;
        }
        return message + "\n" + String.join("\n", history);
    }

    record Prepared(AnaChatGateway llm, Integer userId, AnaTurnRequest turn) {

        String conversationId() {
            return turn.conversationId();
        }
    }
}
