package com.undercontroll.domain.usecase.chat;

import java.util.List;

public interface GetChatSuggestionsPort {
    List<String> execute(boolean refresh);
}
