package com.undercontroll.infrastructure.ai;

import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.undercontroll.domain.support.Texts;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@AutoConfiguration(beforeName = "org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration")
@ConditionalOnClass(Client.class)
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "google-genai")
public class GoogleGenAiClientConfig {

    static final int HTTP_TIMEOUT_MS = 60_000;

    @Bean
    @ConditionalOnMissingBean
    Client googleGenAiClient(Environment environment) {
        Client.Builder clientBuilder = Client.builder().httpOptions(httpOptions());
        String apiKey = Texts.firstNonBlank(
                environment.getProperty("spring.ai.google.genai.api-key"),
                environment.getProperty("GEMINI_API_KEY"),
                environment.getProperty("GOOGLE_API_KEY")
        );
        if (StringUtils.hasText(apiKey)) {
            clientBuilder.apiKey(apiKey);
        } else {
            String projectId = environment.getProperty("spring.ai.google.genai.project-id");
            String location = environment.getProperty("spring.ai.google.genai.location");
            if (StringUtils.hasText(projectId) && StringUtils.hasText(location)) {
                clientBuilder.project(projectId).location(location).vertexAI(true);
            }
        }
        return clientBuilder.build();
    }

    static HttpOptions httpOptions() {
        return HttpOptions.builder()
                .timeout(HTTP_TIMEOUT_MS)
                .retryOptions(HttpRetryOptions.builder().attempts(1).build())
                .build();
    }
}
