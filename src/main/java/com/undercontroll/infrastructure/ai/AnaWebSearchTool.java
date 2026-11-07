package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.support.Durations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
public class AnaWebSearchTool {

    static final String NO_RESULT = "Nenhum resultado útil para essa busca.";

    private final RestClient restClient;

    public AnaWebSearchTool(RestClient.Builder restClientBuilder) {
        this(restClientBuilder.clone()
                .requestFactory(requestFactory())
                .baseUrl("https://api.duckduckgo.com")
                .build());
    }

    AnaWebSearchTool(RestClient restClient) {
        this.restClient = restClient;
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        return factory;
    }

    @Tool(name = "search_web", description = """
            Busca na internet (manuais, recall, dica de eletrodoméstico).
            Não use para dados da oficina (pedidos, estoque, avisos) — isso está no resumo ou nas outras ferramentas.
            Só depois de olhar a oficina.
            """)
    public ToolResult<WebResult> searchWeb(
            @ToolParam(description = "Consulta em português sobre o aparelho (manual, recall ou dica)") String query
    ) {
        if (query == null || query.isBlank()) {
            return ToolResult.invalidInput("Informe o que buscar.");
        }
        long started = System.nanoTime();
        int queryChars = query.length();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restClient.get()
                    .uri(uri -> uri
                            .queryParam("q", query)
                            .queryParam("format", "json")
                            .queryParam("no_html", "1")
                            .queryParam("skip_disambig", "1")
                            .build())
                    .retrieve()
                    .body(Map.class);
            if (body == null) {
                log.warn(
                        "Web search returned empty body queryChars={} durationMs={}",
                        queryChars,
                        Durations.millisSince(started)
                );
                return ToolResult.notFound(NO_RESULT);
            }
            WebResult result = parse(body);
            return result == null ? ToolResult.notFound(NO_RESULT) : ToolResult.ok(result);
        } catch (RestClientResponseException ex) {
            String traceId = ToolResult.newTraceId();
            log.warn(
                    "Web search failed queryChars={} status={} traceId={} durationMs={}",
                    queryChars,
                    ex.getStatusCode().value(),
                    traceId,
                    Durations.millisSince(started)
            );
            return ToolResult.unavailable(traceId, !ex.getStatusCode().is4xxClientError());
        } catch (RuntimeException ex) {
            String traceId = ToolResult.newTraceId();
            log.warn(
                    "Web search failed queryChars={} traceId={} durationMs={} cause={}",
                    queryChars,
                    traceId,
                    Durations.millisSince(started),
                    ex.getClass().getSimpleName()
            );
            return ToolResult.unavailable(traceId, true);
        }
    }

    @SuppressWarnings("unchecked")
    static WebResult parse(Map<String, Object> body) {
        String heading = stringValue(body.get("Heading"));
        String summary = stringValue(body.get("AbstractText"));
        if (summary.isBlank()) {
            summary = stringValue(body.get("Abstract"));
        }
        List<String> related = List.of();
        if (body.get("RelatedTopics") instanceof List<?> topics) {
            related = topics.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> stringValue(((Map<String, Object>) item).get("Text")))
                    .filter(text -> !text.isBlank())
                    .limit(3)
                    .toList();
        }
        if (summary.isBlank() && related.isEmpty()) {
            return null;
        }
        String source = stringValue(body.get("AbstractURL"));
        return new WebResult(blankToNull(heading), blankToNull(summary), related, blankToNull(source));
    }

    private static String blankToNull(String value) {
        return value.isBlank() ? null : value;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    public record WebResult(String heading, String summary, List<String> related, String source) {
    }
}
