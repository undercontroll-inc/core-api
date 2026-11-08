package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.exception.LabelReadingUnavailableException;
import com.undercontroll.domain.gateway.LabelExtractionGateway;
import com.undercontroll.domain.model.LabelReading;
import com.undercontroll.domain.model.insight.LlmFailure;
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

@Slf4j
public class SpringAiLabelExtractionAdapter implements LabelExtractionGateway {

    private final ChatClient chatClient;
    private final ChatOptions options;
    private final ObjectMapper objectMapper;
    private final AiFeatureSettings settings;
    private final Resource userPrompt;
    private final FeatureMetrics metrics;

    public SpringAiLabelExtractionAdapter(
            ChatClient chatClient,
            ChatOptions defaultOptions,
            ObjectMapper objectMapper,
            AiFeatureSettings settings,
            Resource userPrompt,
            FeatureMetrics metrics
    ) {
        if (!(defaultOptions instanceof StructuredOutputChatOptions)) {
            throw new IllegalStateException("undercontroll.ai.label-reading.provider=" + settings.provider().label()
                    + " does not support native structured output");
        }
        ChatOptions copy = defaultOptions.copy();
        ((StructuredOutputChatOptions) copy).setOutputSchema(LabelExtractionSchema.json(objectMapper));
        this.chatClient = chatClient;
        this.options = copy;
        this.objectMapper = objectMapper;
        this.settings = settings;
        this.userPrompt = userPrompt;
        this.metrics = metrics;
    }

    @Override
    public LabelReading extract(InvoiceFile image) {
        long started = System.nanoTime();
        String finishReason = null;
        try {
            ChatResponse response = chatClient.prompt()
                    .options(options.copy())
                    .user(user -> user.text(userPrompt, StandardCharsets.UTF_8)
                            .media(MimeType.valueOf(image.mimeType()), new ByteArrayResource(image.content())))
                    .call()
                    .chatResponse();
            finishReason = response == null || response.getResult() == null || response.getResult().getMetadata() == null
                    ? null : response.getResult().getMetadata().getFinishReason();
            String content = response == null || response.getResult() == null ? null : response.getResult().getOutput().getText();
            if (content == null || content.isBlank()) {
                metrics.record(LlmFailure.Kind.INVALID_OUTPUT.name(), finishReason, started);
                throw new LabelReadingUnavailableException();
            }
            LabelReading reading = objectMapper.readValue(content, LabelReadingJson.class).toDomain();
            metrics.record("success", finishReason, started);
            log.info("Label reading provider={} model={} isLabel={} durationMs={}", settings.provider().label(),
                    settings.model(), reading.isLabel(), Durations.millisSince(started));
            return reading;
        } catch (JsonProcessingException ex) {
            metrics.record(LlmFailure.Kind.INVALID_OUTPUT.name(), finishReason, started);
            throw new LabelReadingUnavailableException(ex);
        } catch (LabelReadingUnavailableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            LlmFailure failure = LlmErrorClassifier.classify(ex);
            metrics.record(failure.kind().name(), null, started);
            log.warn("Label reading failed kind={} status={} durationMs={}", failure.kind(), failure.httpStatus(),
                    Durations.millisSince(started));
            throw new LabelReadingUnavailableException(ex);
        }
    }
}
