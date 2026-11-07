package com.undercontroll.domain.model.chat;

public record AppendedMessage(Conversation conversation, Long messageId, int seq) {
}
