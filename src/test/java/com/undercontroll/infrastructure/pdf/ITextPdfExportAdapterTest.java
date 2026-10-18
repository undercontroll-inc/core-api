package com.undercontroll.infrastructure.pdf;

import com.undercontroll.domain.model.ServiceOrderDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ITextPdfExportAdapterTest {

    private final ITextPdfExportAdapter adapter = new ITextPdfExportAdapter();

    @Test
    @DisplayName("renders the service order template to a PDF")
    void rendersPdf() {
        byte[] pdf = adapter.exportOS(new ServiceOrderDocument(
                "12", "12", "NF-1", "10/01/2026", "Loja",
                List.of(new ServiceOrderDocument.ProductInfo("TV LG X1", "220V", "SN1")),
                "ana@x.com", "Rua 1", "1199", "10/01/2026",
                List.of(new ServiceOrderDocument.PartInfo(2L, "Capacitor", "R$ 5,00"), new ServiceOrderDocument.PartInfo(null, null, null)),
                "R$ 55,00", null, "Técnico", true, null, false
        ));

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("renders empty product and part tables")
    void rendersEmptyTables() {
        byte[] pdf = adapter.exportOS(new ServiceOrderDocument(
                "13", null, null, null, null, List.of(), null, null, null, null, null, null, null, null, null, null, null
        ));

        assertThat(pdf).isNotEmpty();
    }
}
