package com.undercontroll.infrastructure.ai;

import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoogleGenAiClientConfigTest {

    @Test
    @DisplayName("caps Gemini HTTP wait and disables SDK retries on 503")
    void httpOptionsFailFast() {
        HttpOptions options = GoogleGenAiClientConfig.httpOptions();
        HttpRetryOptions retry = options.retryOptions().orElseThrow();

        assertEquals(GoogleGenAiClientConfig.HTTP_TIMEOUT_MS, options.timeout().orElseThrow());
        assertEquals(1, retry.attempts().orElseThrow());
    }
}
