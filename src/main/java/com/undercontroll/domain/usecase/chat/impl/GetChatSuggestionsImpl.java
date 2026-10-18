package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.application.dto.chat.ChatSuggestionsResponse;
import com.undercontroll.domain.gateway.AnaSuggestionStore;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.model.chat.ShopSuggestionComposer;
import com.undercontroll.domain.usecase.chat.GetChatSuggestionsPort;
import com.undercontroll.infrastructure.config.AnaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetChatSuggestionsImpl implements GetChatSuggestionsPort {

    private final AnaSuggestionStore anaSuggestionStore;
    private final ShopSnapshotLoader shopSnapshotLoader;
    private final AnaProperties anaProperties;
    private final CurrentUserIdPort currentUserIdPort;

    @Override
    public ChatSuggestionsResponse execute(boolean refresh) {
        Integer userId = currentUserIdPort.require();
        List<String> previous = anaSuggestionStore.findByUserId(userId).orElse(List.of());
        if (!refresh && !previous.isEmpty()) {
            return new ChatSuggestionsResponse(previous);
        }
        int count = Math.max(1, anaProperties.getSuggestionCount());
        List<String> suggestions = ShopSuggestionComposer.groundedQuestions(
                shopSnapshotLoader.load(),
                refresh ? previous : List.of(),
                count
        );
        anaSuggestionStore.save(userId, suggestions);
        return new ChatSuggestionsResponse(suggestions);
    }
}
