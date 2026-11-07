package com.undercontroll.domain.model.chat;

import java.util.List;

public record MessagePage(List<StoredChatMessage> items, String nextCursor) {
}
