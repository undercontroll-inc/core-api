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
import java.util.stream.Collectors;

@Slf4j
public class AnaWebSearchTool {

    private final RestClient restClient;

    public AnaWebSearchTool(RestClient.Builder restClientBuilder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = restClientBuilder.clone()
                .requestFactory(factory)
                .baseUrl("https://api.duckduckgo.com")
                .build();
    }

    @Tool(name = "search_web", description = """
            Busca na internet (manuais, recall, dica de eletrodoméstico).
            Não use para dados da oficina (pedidos, estoque, avisos) — isso está no resumo ou nas outras ferramentas.
            Só depois de olhar a oficina.
            """)
    public String searchWeb(
            @ToolParam(description = "Consulta em português sobre o aparelho (manual, recall ou dica)") String query
    ) {
        if (query == null || query.isBlank()) {
            return "Busca vazia.";
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
                return "Nenhum resultado.";
            }
            return format(body);
        } catch (RestClientResponseException ex) {
            log.warn(
                    "Web search failed queryChars={} status={} durationMs={}",
                    queryChars,
                    ex.getStatusCode().value(),
                    Durations.millisSince(started)
            );
            return "Busca indisponível no momento.";
        } catch (RuntimeException ex) {
            log.warn(
                    "Web search failed queryChars={} durationMs={} cause={}",
                    queryChars,
                    Durations.millisSince(started),
                    ex.getClass().getSimpleName()
            );
            return "Busca indisponível no momento.";
        }
    }

    @SuppressWarnings("unchecked")
    private static String format(Map<String, Object> body) {
        String heading = stringValue(body.get("Heading"));
        String abstractText = stringValue(body.get("AbstractText"));
        if (abstractText.isBlank()) {
            abstractText = stringValue(body.get("Abstract"));
        }
        String url = stringValue(body.get("AbstractURL"));
        String related = "";
        Object relatedTopics = body.get("RelatedTopics");
        if (relatedTopics instanceof List<?> topics) {
            related = topics.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> stringValue(((Map<String, Object>) item).get("Text")))
                    .filter(text -> !text.isBlank())
                    .limit(3)
                    .collect(Collectors.joining(" | "));
        }
        if (abstractText.isBlank() && related.isBlank()) {
            return "Nenhum resultado útil para essa busca.";
        }
        StringBuilder result = new StringBuilder();
        if (!heading.isBlank()) {
            result.append(heading).append(". ");
        }
        if (!abstractText.isBlank()) {
            result.append(abstractText);
        }
        if (!related.isBlank()) {
            if (!result.isEmpty()) {
                result.append(" ");
            }
            result.append(related);
        }
        if (!url.isBlank()) {
            result.append(" Fonte: ").append(url);
        }
        return result.toString().trim();
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
