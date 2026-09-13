package com.undercontroll.domain.gateway;

public interface ChatStreamSink {

    void start(String conversationId);

    void status(String phase);

    void delta(String chunk);

    void complete(String fullContent);

    void error(String code, String message);

    default void onCancel(Runnable cancel) {
    }
}
