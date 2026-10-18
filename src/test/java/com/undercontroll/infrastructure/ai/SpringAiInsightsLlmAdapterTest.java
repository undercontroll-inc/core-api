package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.exception.InsightLlmException;
import com.undercontroll.domain.exception.LlmOutputException;
import com.undercontroll.domain.insight.InsightContract;
import com.undercontroll.domain.model.insight.InsightCandidate;
import com.undercontroll.domain.model.insight.InsightDraft;
import com.undercontroll.domain.model.insight.InsightDraftRequest;
import com.undercontroll.domain.model.insight.InsightGenerationInput;
import com.undercontroll.domain.model.insight.InsightGenerationSettings;
import com.undercontroll.domain.model.insight.InsightMetric;
import com.undercontroll.domain.model.insight.InsightType;
import com.undercontroll.domain.model.insight.LlmFailure;
import com.undercontroll.domain.model.insight.MetricUnit;
import com.undercontroll.domain.model.insight.RepairRelation;
import com.undercontroll.domain.model.market.MatchCoverage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SpringAiInsightsLlmAdapterTest {

    private static final InsightCandidate CANDIDATE = new InsightCandidate("ALERTA_PRECO:MLB-MICROWAVES",
            InsightType.ALERTA_PRECO, "Micro-ondas", "MLB-MICROWAVES", "Micro-ondas", "2026-08", "2026-07", "HIGH",
            RepairRelation.NIVEL_3_CATEGORIA, "vw_market_price_movement",
            List.of(new InsightMetric("variacao_preco_pct", 10.9, MetricUnit.PERCENT)));
    private static final InsightGenerationInput INPUT = new InsightGenerationInput("2026-08", "2026-07",
            MatchCoverage.empty(), List.of(CANDIDATE), List.of(), List.of());
    private static final InsightContract CONTRACT = new InsightContract(3);
    private static final InsightGenerationSettings SETTINGS =
            new InsightGenerationSettings("gemini", "gemini-2.5-flash", "7", 3, 90, "hash");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
    private final GoogleGenAiChatOptions geminiDefaults = GoogleGenAiChatOptions.builder().model("gemini-2.5-flash").build();

    @Test
    @DisplayName("each call sends a native JSON schema whose candidate ids are an enum, on a copy of the defaults")
    void nativeSchemaOnGemini() throws Exception {
        SpringAiInsightsLlmAdapter adapter = adapter(geminiDefaults);

        GoogleGenAiChatOptions options = (GoogleGenAiChatOptions) adapter.structuredOptions(InsightDraftRequest.first(INPUT, CONTRACT));

        assertEquals("application/json", options.getResponseMimeType());
        assertEquals("gemini-2.5-flash", options.getModel());
        JsonNode schema = objectMapper.readTree(options.getOutputSchema());
        assertEquals(InsightContract.VERSION, schema.get("title").asText());
        assertEquals(false, schema.get("additionalProperties").asBoolean());
        JsonNode item = schema.at("/properties/insights/items");
        assertEquals("[\"candidate_id\",\"text\",\"action\"]", item.get("required").toString());
        assertEquals("[\"ALERTA_PRECO:MLB-MICROWAVES\"]", item.at("/properties/candidate_id/enum").toString());
        assertNull(geminiDefaults.getOutputSchema());
    }

    @Test
    @DisplayName("OpenAI receives the schema as a strict json_schema response format")
    void nativeSchemaOnOpenAi() {
        SpringAiInsightsLlmAdapter adapter = adapter(OpenAiChatOptions.builder().model("gpt-4.1-mini").build());

        OpenAiChatOptions options = (OpenAiChatOptions) adapter.structuredOptions(InsightDraftRequest.first(INPUT, CONTRACT));

        assertEquals(ResponseFormat.Type.JSON_SCHEMA, options.getResponseFormat().getType());
        assertTrue(options.getResponseFormat().getJsonSchema().getStrict());
    }

    @Test
    @DisplayName("a model without native structured output is rejected at startup")
    void rejectsUnstructuredOptions() {
        ChatOptions plain = ChatOptions.builder().model("x").build();

        assertThrows(IllegalStateException.class, () -> adapter(plain));
    }

    @Test
    @DisplayName("parses the structured draft")
    @SuppressWarnings("unchecked")
    void parsesDraft() {
        stubResponse("{\"insights\":[{\"candidate_id\":\"ALERTA_PRECO:MLB-MICROWAVES\",\"text\":\"t\",\"action\":\"a\"}]}", "STOP");

        InsightDraft draft = adapter(geminiDefaults).draft(InsightDraftRequest.first(INPUT, CONTRACT));

        assertEquals(List.of(new InsightDraft.Item("ALERTA_PRECO:MLB-MICROWAVES", "t", "a")), draft.insights());
    }

    @Test
    @DisplayName("truncated, empty or malformed output is classified as invalid output")
    void outputErrors() {
        SpringAiInsightsLlmAdapter adapter = adapter(geminiDefaults);
        InsightDraftRequest request = InsightDraftRequest.first(INPUT, CONTRACT);

        stubResponse("{\"insights\":[", "MAX_TOKENS");
        assertEquals(LlmFailure.Kind.INVALID_OUTPUT, assertThrows(InsightLlmException.class,
                () -> adapter.draft(request)).failure().kind());
        stubResponse(" ", "STOP");
        assertEquals(LlmFailure.Kind.INVALID_OUTPUT, assertThrows(InsightLlmException.class,
                () -> adapter.draft(request)).failure().kind());
        stubResponse("not json", "STOP");
        InsightLlmException malformed = assertThrows(InsightLlmException.class, () -> adapter.draft(request));
        assertEquals(LlmFailure.Kind.INVALID_OUTPUT, malformed.failure().kind());
        assertTrue(malformed.getCause() instanceof LlmOutputException);
    }

    @Test
    @DisplayName("provider errors keep their HTTP status and Retry-After")
    @SuppressWarnings("unchecked")
    void providerErrors() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, "12");
        when(chatClient.prompt().options(any(ChatOptions.class)).user(any(Consumer.class)).call().chatResponse())
                .thenThrow(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "slow down", headers, null, null));

        InsightLlmException error = assertThrows(InsightLlmException.class,
                () -> adapter(geminiDefaults).draft(InsightDraftRequest.first(INPUT, CONTRACT)));

        assertEquals(new LlmFailure(LlmFailure.Kind.RATE_LIMITED, 429, Duration.ofSeconds(12)), error.failure());
    }

    @Test
    @DisplayName("no candidates means no model call")
    void noCandidates() {
        ChatClient untouched = mock(ChatClient.class);
        SpringAiInsightsLlmAdapter adapter = new SpringAiInsightsLlmAdapter(untouched, geminiDefaults, objectMapper,
                SETTINGS, new ByteArrayResource(new byte[0]));
        InsightGenerationInput empty = new InsightGenerationInput("2026-08", null, MatchCoverage.empty(), List.of(),
                List.of(), List.of());

        assertTrue(adapter.draft(InsightDraftRequest.first(empty, CONTRACT)).insights().isEmpty());
        verifyNoInteractions(untouched);
    }

    @Test
    @DisplayName("a repair call sends the previous draft and every validation error")
    void repairParams() {
        SpringAiInsightsLlmAdapter adapter = adapter(geminiDefaults);
        InsightDraft previous = new InsightDraft(List.of(new InsightDraft.Item(CANDIDATE.id(), "caiu 6%", "a")));

        Map<String, Object> first = adapter.params(InsightDraftRequest.first(INPUT, CONTRACT));
        Map<String, Object> repair = adapter.params(InsightDraftRequest.repair(INPUT, CONTRACT, previous,
                List.of("insights[0].text writes digits directly")));

        assertEquals("mês de agosto de 2026", first.get("mesAtual"));
        assertEquals(3, first.get("maxInsights"));
        assertEquals("", first.get("correcao"));
        assertTrue(first.get("candidatos").toString().contains("\"variacao_preco_pct\":\"10,9%\""));
        assertTrue(first.get("candidatos").toString().contains("\"valores\":{\"variacao_preco_pct\":10.9}"));
        String correction = repair.get("correcao").toString();
        assertTrue(correction.contains("\"text\":\"caiu 6%\""));
        assertTrue(correction.contains("- insights[0].text writes digits directly"));
    }

    @SuppressWarnings("unchecked")
    private void stubResponse(String content, String finishReason) {
        ChatResponse response = new ChatResponse(List.of(new Generation(new AssistantMessage(content),
                ChatGenerationMetadata.builder().finishReason(finishReason).build())));
        when(chatClient.prompt().options(any(ChatOptions.class)).user(any(Consumer.class)).call().chatResponse())
                .thenReturn(response);
    }

    private SpringAiInsightsLlmAdapter adapter(ChatOptions defaults) {
        return new SpringAiInsightsLlmAdapter(chatClient, defaults, objectMapper, SETTINGS,
                new ByteArrayResource("<mesAtual>".getBytes()));
    }
}
