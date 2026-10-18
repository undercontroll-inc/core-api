package com.undercontroll.domain.model.chat;

public record AnaTurnRequest(String conversationId, String message, String briefing, boolean webSearch) {
}
