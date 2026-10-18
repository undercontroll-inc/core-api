package com.undercontroll.infrastructure.ai;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.Exceptions;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeoutException;

final class AnaFinishReason {

    static final String MAX_TOKENS = "max_tokens";
    static final String INTERRUPTED = "interrupted";
    static final String TIMEOUT = "timeout";

    private static final Set<String> NORMAL = Set.of("STOP", "TOOL_CALLS", "FINISH_REASON_UNSPECIFIED");
    private static final Set<String> TRUNCATED = Set.of("MAX_TOKENS", "LENGTH");

    private AnaFinishReason() {
    }

    static String of(ChatResponse response) {
        if (response == null) {
            return null;
        }
        Generation result = response.getResult();
        if (result == null || result.getMetadata() == null) {
            return null;
        }
        return result.getMetadata().getFinishReason();
    }

    static String of(Throwable error) {
        return Exceptions.unwrap(error) instanceof TimeoutException ? TIMEOUT : INTERRUPTED;
    }

    static String partialReason(String finishReason) {
        if (finishReason == null || finishReason.isBlank()) {
            return null;
        }
        String normalized = finishReason.trim().toUpperCase(Locale.ROOT);
        if (NORMAL.contains(normalized)) {
            return null;
        }
        if (TRUNCATED.contains(normalized)) {
            return MAX_TOKENS;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
