package com.undercontroll.domain.gateway;

public interface ChatStreamSink {

    void start(String conversationId);

    void status(String phase);

    void delta(String chunk);

    void complete(String fullContent);

    void partial(String content, String reason);

    void error(String code, String message);

    default void start(String conversationId, Long userMessageId) {
        start(conversationId);
    }

    default void complete(String fullContent, Long messageId) {
        complete(fullContent);
    }

    default void partial(String content, String reason, Long messageId) {
        partial(content, reason);
    }

    default void onCancel(Runnable cancel) {
    }
}
