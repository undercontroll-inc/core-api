package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.exception.ConversationNotFoundException;
import com.undercontroll.domain.exception.InvalidChatHistoryRequestException;
import com.undercontroll.domain.model.chat.ConversationKey;

import java.util.regex.Pattern;

final class ConversationRequests {

    static final int MAX_LIMIT = 50;
    private static final Pattern ID = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private ConversationRequests() {
    }

    static String id(String conversationId) {
        if (conversationId == null || !ID.matcher(conversationId).matches()) {
            throw new ConversationNotFoundException(conversationId);
        }
        return conversationId;
    }

    static ConversationKey key(Integer userId, String conversationId) {
        return ConversationKey.of(userId, id(conversationId));
    }

    static int limit(Integer limit, int defaultLimit) {
        if (limit == null) {
            return defaultLimit;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new InvalidChatHistoryRequestException("limit", "limit must be between 1 and " + MAX_LIMIT);
        }
        return limit;
    }
}
