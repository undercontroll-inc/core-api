package com.undercontroll.infrastructure.pdf;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.undercontroll.domain.gateway.PdfInspectorGateway;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.OptionalInt;

@Component
public class ITextPdfInspector implements PdfInspectorGateway {

    @Override
    public OptionalInt pageCount(byte[] pdf) {
        try (PdfDocument document = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            return OptionalInt.of(document.getNumberOfPages());
        } catch (IOException | RuntimeException ex) {
            return OptionalInt.empty();
        }
    }
}
