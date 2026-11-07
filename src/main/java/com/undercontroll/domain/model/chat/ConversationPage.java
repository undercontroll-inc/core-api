package com.undercontroll.domain.model.chat;

import java.util.List;

public record ConversationPage(List<Conversation> items, String nextCursor) {
}
