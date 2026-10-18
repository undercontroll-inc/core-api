package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.gateway.AnaSuggestionStore;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.gateway.ShopSnapshotLoader;
import com.undercontroll.domain.model.chat.ShopSuggestionComposer;
import com.undercontroll.domain.usecase.chat.GetChatSuggestionsPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetChatSuggestionsImpl implements GetChatSuggestionsPort {

    private final AnaSuggestionStore anaSuggestionStore;
    private final ShopSnapshotLoader shopSnapshotLoader;
    private final CurrentUserIdPort currentUserIdPort;
    private final int suggestionCount;

    public GetChatSuggestionsImpl(
            AnaSuggestionStore anaSuggestionStore,
            ShopSnapshotLoader shopSnapshotLoader,
            CurrentUserIdPort currentUserIdPort,
            @Value("${undercontroll.ana.suggestion-count:4}") int suggestionCount
    ) {
        this.anaSuggestionStore = anaSuggestionStore;
        this.shopSnapshotLoader = shopSnapshotLoader;
        this.currentUserIdPort = currentUserIdPort;
        this.suggestionCount = suggestionCount;
    }

    @Override
    public List<String> execute(boolean refresh) {
        Integer userId = currentUserIdPort.require();
        List<String> previous = anaSuggestionStore.findByUserId(userId).orElse(List.of());
        if (!refresh && !previous.isEmpty()) {
            return previous;
        }
        int count = Math.max(1, suggestionCount);
        List<String> suggestions = ShopSuggestionComposer.groundedQuestions(
                shopSnapshotLoader.load(),
                refresh ? previous : List.of(),
                count
        );
        anaSuggestionStore.save(userId, suggestions);
        return suggestions;
    }
}
