package com.undercontroll.infrastructure.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsightsUserPromptTemplateTest {

    @Test
    @DisplayName("user prompt interpolates the month, the limit, the candidates and the optional correction")
    void renders() throws IOException {
        String rendered = render(Map.of(
                "mesAtual", "mês de agosto de 2026",
                "maxInsights", 5,
                "candidatos", "[{\"candidate_id\":\"ALERTA_PRECO:MLB-MICROWAVES\"}]",
                "correcao", ""
        ));

        assertTrue(rendered.startsWith("Escreva os insights do mês de agosto de 2026"));
        assertTrue(rendered.contains("no máximo 5 candidatos"));
        assertTrue(rendered.contains("\"candidate_id\":\"ALERTA_PRECO:MLB-MICROWAVES\""));
        assertFalse(rendered.contains("<"));
        assertFalse(rendered.contains("CORREÇÃO"));
        assertTrue(render(Map.of("mesAtual", "m", "maxInsights", 1, "candidatos", "[]",
                "correcao", "CORREÇÃO OBRIGATÓRIA")).contains("CORREÇÃO OBRIGATÓRIA"));
    }

    @Test
    @DisplayName("system prompt has no template delimiters and requires placeholders for every number")
    void systemPrompt() throws IOException {
        String system = new ClassPathResource("prompts/insights-system-prompt.txt").getContentAsString(StandardCharsets.UTF_8);

        assertFalse(system.contains("<"));
        assertFalse(system.contains(">"));
        assertTrue(system.contains("Não escreva nenhum dígito fora de placeholder"));
    }

    private static String render(Map<String, Object> variables) throws IOException {
        String template = new ClassPathResource("prompts/insights-user-prompt.txt").getContentAsString(StandardCharsets.UTF_8);
        return PromptTemplate.builder()
                .template(template)
                .variables(variables)
                .renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
                .build()
                .render();
    }
}
