package com.undercontroll.domain.model.chat;

public record ChatMessage(Role role, String text) {

    public enum Role {
        USER,
        ASSISTANT
    }

    public static ChatMessage user(String text) {
        return new ChatMessage(Role.USER, text);
    }

    public static ChatMessage assistant(String text) {
        return new ChatMessage(Role.ASSISTANT, text);
    }
}
