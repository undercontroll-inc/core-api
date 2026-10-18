package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.undercontroll.domain.insight.InsightContract;

import java.util.List;

public final class InsightDraftSchema {

    private InsightDraftSchema() {
    }

    public static String json(ObjectMapper objectMapper, InsightContract contract, List<String> candidateIds) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("type", "object");
        ObjectNode itemProperties = item.putObject("properties");
        ObjectNode candidate = itemProperties.putObject("candidate_id");
        candidate.put("type", "string");
        candidate.put("description", "Id de um dos candidatos recebidos.");
        ArrayNode ids = candidate.putArray("enum");
        candidateIds.forEach(ids::add);
        ObjectNode text = itemProperties.putObject("text");
        text.put("type", "string");
        text.put("description", "Narrativa em 2 a 4 frases, de " + InsightContract.MIN_TEXT_LENGTH + " a "
                + InsightContract.MAX_TEXT_LENGTH + " caracteres depois de preencher os placeholders. "
                + "Números, meses e nomes só como {{placeholder}} do candidato.");
        ObjectNode action = itemProperties.putObject("action");
        action.put("type", "string");
        action.put("description", "Uma ação concreta para a oficina, até " + InsightContract.MAX_ACTION_LENGTH
                + " caracteres, com as mesmas regras de placeholder.");
        required(item, "candidate_id", "text", "action");

        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");
        root.put("title", InsightContract.VERSION);
        ObjectNode insights = root.putObject("properties").putObject("insights");
        insights.put("type", "array");
        insights.put("description", "No máximo " + contract.maxInsights() + " insights, um por candidato.");
        insights.set("items", item);
        required(root, "insights");
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to build the insight draft schema", ex);
        }
    }

    private static void required(ObjectNode node, String... fields) {
        ArrayNode required = node.putArray("required");
        for (String field : fields) {
            required.add(field);
        }
        node.put("additionalProperties", false);
    }
}
