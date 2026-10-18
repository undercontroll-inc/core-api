package com.undercontroll.domain.model.chat;

public record AnaReply(String content, State state, String reason) {

    public enum State {
        COMPLETED,
        PARTIAL
    }

    public static AnaReply completed(String content) {
        return new AnaReply(content, State.COMPLETED, null);
    }

    public static AnaReply partial(String content, String reason) {
        return new AnaReply(content, State.PARTIAL, reason);
    }

    public boolean isCompleted() {
        return state == State.COMPLETED;
    }
}
