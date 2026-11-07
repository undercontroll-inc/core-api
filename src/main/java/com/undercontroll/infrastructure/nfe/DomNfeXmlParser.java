package com.undercontroll.infrastructure.nfe;

import com.undercontroll.domain.exception.InvalidNfeXmlException;
import com.undercontroll.domain.gateway.NfeXmlGateway;
import com.undercontroll.domain.model.stockimport.InvoiceExtraction;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class DomNfeXmlParser implements NfeXmlGateway {

    static final ZoneOffset BRASILIA = ZoneOffset.ofHours(-3);

    @Override
    public InvoiceExtraction parse(byte[] xml) {
        Element infNFe = first(document(xml).getDocumentElement(), "infNFe")
                .orElseThrow(() -> new InvalidNfeXmlException("XML is not an NF-e (infNFe not found)"));
        Element ide = first(infNFe, "ide").orElseThrow(() -> new InvalidNfeXmlException("NF-e has no ide section"));
        Element emit = first(infNFe, "emit").orElse(null);
        List<InvoiceExtraction.ExtractedItem> items = new ArrayList<>();
        NodeList details = infNFe.getElementsByTagNameNS("*", "det");
        for (int i = 0; i < details.getLength(); i++) {
            first((Element) details.item(i), "prod").ifPresent(prod -> items.add(new InvoiceExtraction.ExtractedItem(
                    text(prod, "cProd"),
                    text(prod, "xProd"),
                    decimal(prod, "qCom"),
                    text(prod, "uCom"),
                    decimal(prod, "vUnCom"),
                    decimal(prod, "vProd"),
                    1.0
            )));
        }
        if (items.isEmpty()) {
            throw new InvalidNfeXmlException("NF-e has no items (det)");
        }
        String id = infNFe.getAttribute("Id");
        String accessKey = id != null && id.startsWith("NFe") ? id.substring(3) : null;
        BigDecimal total = first(infNFe, "ICMSTot").map(tot -> decimal(tot, "vNF")).orElse(null);
        return new InvoiceExtraction(
                true,
                emit == null ? null : firstNonBlank(text(emit, "xNome"), text(emit, "xFant")),
                emit == null ? null : firstNonBlank(text(emit, "CNPJ"), text(emit, "CPF")),
                text(ide, "nNF"),
                text(ide, "serie"),
                issuedAt(firstNonBlank(text(ide, "dhEmi"), text(ide, "dEmi"))),
                accessKey,
                total,
                items
        );
    }

    private static Document document(byte[] xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(null);
            return builder.parse(new ByteArrayInputStream(xml));
        } catch (ParserConfigurationException ex) {
            throw new IllegalStateException("XML parser is not available", ex);
        } catch (SAXException | IOException ex) {
            throw new InvalidNfeXmlException("XML could not be parsed", ex);
        }
    }

    private static java.util.Optional<Element> first(Element parent, String name) {
        if (parent == null) {
            return java.util.Optional.empty();
        }
        if (name.equals(parent.getLocalName())) {
            return java.util.Optional.of(parent);
        }
        NodeList nodes = parent.getElementsByTagNameNS("*", name);
        return nodes.getLength() == 0 ? java.util.Optional.empty() : java.util.Optional.of((Element) nodes.item(0));
    }

    private static String text(Element parent, String name) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element && name.equals(element.getLocalName())) {
                String value = element.getTextContent();
                return value == null || value.isBlank() ? null : value.strip();
            }
        }
        return null;
    }

    private static BigDecimal decimal(Element parent, String name) {
        String value = text(parent, name);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new InvalidNfeXmlException("NF-e field " + name + " is not a number: " + value);
        }
    }

    static OffsetDateTime issuedAt(String value) {
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException notOffset) {
            try {
                return LocalDate.parse(value).atStartOfDay().atOffset(BRASILIA);
            } catch (DateTimeParseException invalid) {
                return null;
            }
        }
    }

    private static String firstNonBlank(String a, String b) {
        return a != null ? a : b;
    }
}
