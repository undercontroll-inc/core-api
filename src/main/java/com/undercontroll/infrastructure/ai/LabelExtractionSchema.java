package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class LabelExtractionSchema {

    public static final String VERSION = "label-reading-v1";

    private LabelExtractionSchema() {
    }

    public static String json(ObjectMapper objectMapper) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");
        root.put("title", VERSION);
        ObjectNode properties = root.putObject("properties");
        ObjectNode isLabel = properties.putObject("is_label");
        isLabel.put("type", "boolean");
        isLabel.put("description", "true se a foto mostra a etiqueta de identificação de um aparelho.");
        nullable(properties, "type", "Tipo de aparelho em português, singular e minúsculo.");
        nullable(properties, "brand", "Marca.");
        nullable(properties, "model", "Modelo como está na etiqueta.");
        nullable(properties, "serial", "Número de série.");
        ObjectNode voltage = properties.putObject("voltage");
        ArrayNode voltageTypes = voltage.putArray("type");
        voltageTypes.add("string").add("null");
        ArrayNode values = voltage.putArray("enum");
        values.add("127V").add("220V").add("BIVOLT").addNull();
        voltage.put("description", "Tensão do aparelho.");
        ObjectNode confidence = properties.putObject("confidence");
        confidence.put("type", "number");
        confidence.put("description", "Confiança de 0 a 1 na leitura.");
        ArrayNode required = root.putArray("required");
        for (String field : new String[]{"is_label", "type", "brand", "model", "serial", "voltage", "confidence"}) {
            required.add(field);
        }
        root.put("additionalProperties", false);
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to build the label reading schema", ex);
        }
    }

    private static void nullable(ObjectNode properties, String name, String description) {
        ObjectNode node = properties.putObject(name);
        node.putArray("type").add("string").add("null");
        node.put("description", description);
    }
}
