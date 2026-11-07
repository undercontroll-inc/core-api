package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class InvoiceExtractionSchema {

    public static final String VERSION = "stock-import-extraction-v1";
    static final int MAX_ITEMS = 300;

    private InvoiceExtractionSchema() {
    }

    public static String json(ObjectMapper objectMapper) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("type", "object");
        ObjectNode itemProperties = item.putObject("properties");
        nullable(itemProperties, "supplier_code", "string", "Código do produto no fornecedor.");
        property(itemProperties, "description", "string", "Descrição do produto como está no documento.");
        nullable(itemProperties, "quantity", "number", "Quantidade comprada.");
        nullable(itemProperties, "unit", "string", "Unidade comercial (UN, PC, CX, M...).");
        nullable(itemProperties, "unit_price", "number", "Valor unitário.");
        nullable(itemProperties, "total", "number", "Valor total da linha.");
        property(itemProperties, "confidence", "number", "Confiança de 0 a 1 na leitura da linha.");
        required(item, "supplier_code", "description", "quantity", "unit", "unit_price", "total", "confidence");

        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");
        root.put("title", VERSION);
        ObjectNode properties = root.putObject("properties");
        property(properties, "is_invoice", "boolean", "true se o arquivo é nota fiscal, cupom ou recibo de compra.");
        nullable(properties, "supplier_name", "string", "Razão social ou nome do emitente.");
        nullable(properties, "supplier_cnpj", "string", "CNPJ ou CPF do emitente, só dígitos.");
        nullable(properties, "invoice_number", "string", "Número da nota.");
        nullable(properties, "invoice_series", "string", "Série da nota.");
        nullable(properties, "issued_at", "string", "Data de emissão AAAA-MM-DD.");
        nullable(properties, "access_key", "string", "Chave de acesso com 44 dígitos.");
        nullable(properties, "total", "number", "Valor total da nota.");
        ObjectNode items = properties.putObject("items");
        items.put("type", "array");
        items.put("description", "Itens na ordem do documento, no máximo " + MAX_ITEMS + ".");
        items.set("items", item);
        required(root, "is_invoice", "supplier_name", "supplier_cnpj", "invoice_number", "invoice_series",
                "issued_at", "access_key", "total", "items");
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to build the invoice extraction schema", ex);
        }
    }

    private static void property(ObjectNode properties, String name, String type, String description) {
        ObjectNode node = properties.putObject(name);
        node.put("type", type);
        node.put("description", description);
    }

    private static void nullable(ObjectNode properties, String name, String type, String description) {
        ObjectNode node = properties.putObject(name);
        node.putArray("type").add(type).add("null");
        node.put("description", description);
    }

    private static void required(ObjectNode node, String... fields) {
        ArrayNode required = node.putArray("required");
        for (String field : fields) {
            required.add(field);
        }
        node.put("additionalProperties", false);
    }
}
