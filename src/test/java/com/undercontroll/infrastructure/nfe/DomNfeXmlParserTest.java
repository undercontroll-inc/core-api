package com.undercontroll.infrastructure.nfe;

import com.undercontroll.domain.exception.InvalidNfeXmlException;
import com.undercontroll.domain.model.stockimport.InvoiceExtraction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomNfeXmlParserTest {

    private final DomNfeXmlParser parser = new DomNfeXmlParser();

    @Test
    @DisplayName("parses supplier, header, items and access key from an NF-e 4.00 nfeProc")
    void parsesNfeProc() throws IOException {
        InvoiceExtraction extraction = parser.parse(sample());

        assertTrue(extraction.invoice());
        assertEquals("Distribuidora de Pecas Eletro LTDA", extraction.supplierName());
        assertEquals("12345678000195", extraction.supplierCnpj());
        assertEquals("1234", extraction.number());
        assertEquals("1", extraction.series());
        assertEquals(OffsetDateTime.parse("2026-11-03T10:15:00-03:00"), extraction.issuedAt());
        assertEquals("35261112345678000195550010000012341000012345", extraction.accessKey());
        assertEquals(0, new BigDecimal("204.80").compareTo(extraction.total()));
        assertEquals(2, extraction.items().size());
        InvoiceExtraction.ExtractedItem motor = extraction.items().get(1);
        assertEquals("MOT-LIQ-220", motor.supplierCode());
        assertEquals("Motor liquidificador 220V", motor.description());
        assertEquals(0, new BigDecimal("2").compareTo(motor.quantity()));
        assertEquals("PC", motor.unit());
        assertEquals(0, new BigDecimal("89.90").compareTo(motor.unitPrice()));
        assertEquals(0, new BigDecimal("179.80").compareTo(motor.total()));
        assertEquals(1.0, motor.confidence());
    }

    @Test
    @DisplayName("rejects XML that is not an NF-e")
    void rejectsOtherXml() {
        byte[] xml = "<note><to>x</to></note>".getBytes(StandardCharsets.UTF_8);
        assertThrows(InvalidNfeXmlException.class, () -> parser.parse(xml));
    }

    @Test
    @DisplayName("rejects documents with a DOCTYPE (XXE)")
    void rejectsDoctype() {
        byte[] xml = ("<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]>"
                + "<infNFe><ide><nNF>&e;</nNF></ide></infNFe>").getBytes(StandardCharsets.UTF_8);
        assertThrows(InvalidNfeXmlException.class, () -> parser.parse(xml));
    }

    @Test
    @DisplayName("rejects malformed XML")
    void rejectsMalformed() {
        assertThrows(InvalidNfeXmlException.class, () -> parser.parse("<infNFe>".getBytes(StandardCharsets.UTF_8)));
    }

    static byte[] sample() throws IOException {
        try (InputStream in = DomNfeXmlParserTest.class.getResourceAsStream("/stockimport/nfe-sample.xml")) {
            return in.readAllBytes();
        }
    }
}
