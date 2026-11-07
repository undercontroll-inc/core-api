package com.undercontroll.domain.gateway;

public interface ConversationListVersion {

    long UNAVAILABLE = -1L;

    long current(Integer userId);

    void bump(Integer userId);
}
