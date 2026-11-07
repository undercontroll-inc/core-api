package com.undercontroll.infrastructure.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AnaWebSearchToolTest {

    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://api.duckduckgo.com");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final AnaWebSearchTool tool = new AnaWebSearchTool(builder.build());

    @Test
    @DisplayName("rejects a blank search query without calling the internet")
    void blankQuery() {
        assertEquals(ToolResult.Status.INVALID_INPUT, new AnaWebSearchTool(RestClient.builder()).searchWeb("  ").status());
    }

    @Test
    @DisplayName("returns structured fields with the source")
    void structuredResult() {
        server.expect(queryParam("q", "recall%20liquidificador"))
                .andRespond(withSuccess("""
                        {"Heading":"Recall","AbstractText":"Lote 2024 com defeito.","AbstractURL":"https://x.test/recall",
                         "RelatedTopics":[{"Text":"Troca gratuita"},{"Text":" "},"ignored"]}
                        """, MediaType.APPLICATION_JSON));

        ToolResult<AnaWebSearchTool.WebResult> result = tool.searchWeb("recall liquidificador");

        assertEquals(ToolResult.Status.OK, result.status());
        assertEquals("Recall", result.data().heading());
        assertEquals("Lote 2024 com defeito.", result.data().summary());
        assertEquals(java.util.List.of("Troca gratuita"), result.data().related());
        assertEquals("https://x.test/recall", result.data().source());
        assertNotNull(result.dataPolicy());
    }

    @Test
    @DisplayName("empty answer is not_found, not an error")
    void emptyAnswer() {
        server.expect(queryParam("q", "nada"))
                .andRespond(withSuccess("{\"Heading\":\"\",\"Abstract\":\"\",\"RelatedTopics\":[]}", MediaType.APPLICATION_JSON));

        assertEquals(ToolResult.Status.NOT_FOUND, tool.searchWeb("nada").status());
    }

    @Test
    @DisplayName("provider failures are unavailable; client errors are not retryable")
    void failures() {
        server.expect(queryParam("q", "a")).andRespond(withServerError());
        server.expect(queryParam("q", "b")).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        ToolResult<AnaWebSearchTool.WebResult> serverError = tool.searchWeb("a");
        ToolResult<AnaWebSearchTool.WebResult> clientError = tool.searchWeb("b");

        assertEquals(ToolResult.Status.UNAVAILABLE, serverError.status());
        assertTrue(serverError.retryable());
        assertNotNull(serverError.traceId());
        assertEquals(ToolResult.Status.UNAVAILABLE, clientError.status());
        assertFalse(clientError.retryable());
    }

    @Test
    @DisplayName("falls back to Abstract when AbstractText is blank")
    void abstractFallback() {
        AnaWebSearchTool.WebResult result = AnaWebSearchTool.parse(java.util.Map.of("Abstract", "Resumo"));

        assertEquals("Resumo", result.summary());
        assertEquals(null, result.source());
    }
}
