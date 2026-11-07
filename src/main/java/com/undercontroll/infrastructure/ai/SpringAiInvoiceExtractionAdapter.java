package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.exception.InvoiceExtractionException;
import com.undercontroll.domain.exception.LlmOutputException;
import com.undercontroll.domain.gateway.InvoiceExtractionGateway;
import com.undercontroll.domain.model.insight.LlmFailure;
import com.undercontroll.domain.model.stockimport.InvoiceExtraction;
import com.undercontroll.domain.model.stockimport.InvoiceFile;
import com.undercontroll.domain.support.Durations;
import com.undercontroll.infrastructure.ai.config.AiFeatureSettings;
import com.undercontroll.infrastructure.metrics.FeatureMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.StructuredOutputChatOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

@Slf4j
public class SpringAiInvoiceExtractionAdapter implements InvoiceExtractionGateway {

    private static final Set<String> TRUNCATED = Set.of("MAX_TOKENS", "LENGTH");

    private final ChatClient chatClient;
    private final ChatOptions options;
    private final ObjectMapper objectMapper;
    private final AiFeatureSettings settings;
    private final Resource userPrompt;
    private final FeatureMetrics metrics;

    public SpringAiInvoiceExtractionAdapter(
            ChatClient chatClient,
            ChatOptions defaultOptions,
            ObjectMapper objectMapper,
            AiFeatureSettings settings,
            Resource userPrompt,
            FeatureMetrics metrics
    ) {
        if (!(defaultOptions instanceof StructuredOutputChatOptions)) {
            throw new IllegalStateException("undercontroll.ai.stock-import.provider=" + settings.provider().label()
                    + " does not support native structured output");
        }
        this.chatClient = chatClient;
        this.options = structuredOptions(defaultOptions, objectMapper);
        this.objectMapper = objectMapper;
        this.settings = settings;
        this.userPrompt = userPrompt;
        this.metrics = metrics;
    }

    @Override
    public InvoiceExtraction extract(InvoiceFile file) {
        long started = System.nanoTime();
        try {
            return request(file, started);
        } catch (InvoiceExtractionException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            LlmFailure failure = LlmErrorClassifier.classify(ex);
            metrics.record(failure.kind().name(), null, started);
            log.warn("Invoice extraction failed sourceType={} kind={} status={} durationMs={}",
                    file.sourceType(), failure.kind(), failure.httpStatus(), Durations.millisSince(started));
            throw new InvoiceExtractionException(failure, failure.kind() + ": " + ex.getMessage(), ex);
        }
    }

    private InvoiceExtraction request(InvoiceFile file, long started) {
        ByteArrayResource media = new ByteArrayResource(file.content());
        ChatResponse response = chatClient.prompt()
                .options(options.copy())
                .user(user -> user.text(userPrompt, StandardCharsets.UTF_8)
                        .media(MimeType.valueOf(file.mimeType()), media))
                .call()
                .chatResponse();
        String finishReason = finishReason(response);
        log.info("Invoice extraction sourceType={} provider={} model={} finishReason={} durationMs={}",
                file.sourceType(), settings.provider().label(), settings.model(), finishReason,
                Durations.millisSince(started));
        if (finishReason != null && TRUNCATED.contains(finishReason.toUpperCase(Locale.ROOT))) {
            metrics.record(LlmFailure.Kind.INVALID_OUTPUT.name(), finishReason, started);
            throw new InvoiceExtractionException(LlmFailure.of(LlmFailure.Kind.INVALID_OUTPUT),
                    "Invoice extraction was truncated (finish reason " + finishReason + ")", null);
        }
        String content = response == null || response.getResult() == null
                ? null
                : response.getResult().getOutput().getText();
        try {
            if (content == null || content.isBlank()) {
                throw new LlmOutputException("Invoice extraction is empty");
            }
            InvoiceExtraction extraction = objectMapper.readValue(content, InvoiceExtractionJson.class).toDomain();
            metrics.record("success", finishReason, started);
            return extraction;
        } catch (JsonProcessingException | LlmOutputException ex) {
            metrics.record(LlmFailure.Kind.INVALID_OUTPUT.name(), finishReason, started);
            throw new InvoiceExtractionException(LlmFailure.of(LlmFailure.Kind.INVALID_OUTPUT),
                    "Invoice extraction is not valid JSON for " + InvoiceExtractionSchema.VERSION, ex);
        }
    }

    static ChatOptions structuredOptions(ChatOptions defaults, ObjectMapper objectMapper) {
        ChatOptions copy = defaults.copy();
        ((StructuredOutputChatOptions) copy).setOutputSchema(InvoiceExtractionSchema.json(objectMapper));
        return copy;
    }

    private static String finishReason(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getMetadata() == null) {
            return null;
        }
        return response.getResult().getMetadata().getFinishReason();
    }
}
