package com.undercontroll.infrastructure.adapter;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.undercontroll.infrastructure.web.dto.ExportOrderRequest;
import com.undercontroll.domain.exception.PdfGenerationException;
import com.undercontroll.domain.exception.TempFileException;
import com.undercontroll.domain.exception.TemplateLoadException;
import com.undercontroll.application.service.PdfExportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
public class PdfBoxService implements PdfExportService {

    private static final String TEMP_FILE_PREFIX = "os_";
    private static final String HTML_EXTENSION = ".html";
    private static final String TEMPLATE_PATH = "templates/os.html";

    @Override
    public byte[] exportOS(ExportOrderRequest request) {
        log.info("Iniciando exportação da OS: {}", request.numeroOs());

        String osContent = loadTemplate();
        String processedHtml = processTemplate(osContent, request);

        Path tempHtmlFile = null;

        try {
            tempHtmlFile = createTempHtmlFile(processedHtml, request.numeroOs());
            log.debug("Arquivo HTML temporário criado: {}", tempHtmlFile);

            byte[] pdfBytes = convertHtmlToPdf(tempHtmlFile);
            log.info("PDF gerado com sucesso para OS: {}", request.numeroOs());

            return pdfBytes;
        } finally {
            deleteTempFileSafely(tempHtmlFile);
        }
    }

    private String processTemplate(String template, ExportOrderRequest request) {
        String listaProdutos = generateProductsHtml(request.produtos());
        String tabelaPecas = generatePartsTableHtml(request.pecas());

        return template.replace("${numero_os}", nullSafe(request.numeroOs()))
                       .replace("${os}", nullSafe(request.os()))
                       .replace("${nf}", nullSafe(request.nf()))
                       .replace("${data}", nullSafe(request.data()))
                       .replace("${loja}", nullSafe(request.loja()))
                       .replace("${lista_produtos}", listaProdutos)
                       .replace("${nome}", nullSafe(request.nome()))
                       .replace("${endereco}", nullSafe(request.endereco()))
                       .replace("${telefone}", nullSafe(request.telefone()))
                       .replace("${recepcao}", nullSafe(request.recepcao()))
                       .replace("${tabela_pecas}", tabelaPecas)
                       .replace("${total}", nullSafe(request.total()))
                       .replace("${data_conserto}", nullSafe(request.dataConserto()))
                       .replace("${tecnico}", nullSafe(request.tecnico()))
                       .replace("${garantia_fabrica}", getCheckboxClass(request.garantiaFabrica()))
                       .replace("${orcamento}", getCheckboxClass(request.orcamento()))
                       .replace("${retorno_garantia}", getCheckboxClass(request.retornoGarantia()));
    }

    private Path createTempHtmlFile(String htmlContent, String osNumber) {
        try {
            Path tempFile = Files.createTempFile(TEMP_FILE_PREFIX + osNumber + "_", HTML_EXTENSION);
            Files.writeString(tempFile, htmlContent, StandardCharsets.UTF_8);
            return tempFile;
        } catch (IOException e) {
            log.error("Erro ao criar arquivo HTML temporário para OS {}: {}", osNumber, e.getMessage());
            throw new TempFileException("Falha ao criar arquivo HTML temporário para OS: " + osNumber, e);
        }
    }

    private byte[] convertHtmlToPdf(Path htmlFilePath) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ConverterProperties props = new ConverterProperties();
            props.setBaseUri(htmlFilePath.getParent().toString() + "/");

            String htmlContent = Files.readString(htmlFilePath, StandardCharsets.UTF_8);
            HtmlConverter.convertToPdf(htmlContent, outputStream, props);

            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Erro ao converter HTML para PDF: {}", e.getMessage());
            throw new PdfGenerationException("Falha ao converter HTML para PDF", e);
        }
    }

    private void deleteTempFileSafely(Path tempFile) {
        if (tempFile == null) {
            return;
        }

        try {
            boolean deleted = Files.deleteIfExists(tempFile);
            if (deleted) {
                log.debug("Arquivo temporário excluído com sucesso: {}", tempFile);
            } else {
                log.warn("Arquivo temporário não encontrado para exclusão: {}", tempFile);
            }
        } catch (IOException e) {
            log.warn("Não foi possível excluir o arquivo temporário {}: {}", tempFile, e.getMessage());
        }
    }

    private String loadTemplate() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(TEMPLATE_PATH)) {
            if (inputStream == null) {
                throw new TemplateLoadException("Template de OS não encontrado em: " + TEMPLATE_PATH);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Erro ao carregar template: {}", e.getMessage());
            throw new TemplateLoadException("Falha ao carregar template de OS", e);
        }
    }

    private String generateProductsHtml(List<ExportOrderRequest.ProductInfo> produtos) {
        if (produtos == null || produtos.isEmpty()) {
            return "<tr><td colspan=\"3\" style=\"text-align: center;\">Nenhum produto cadastrado</td></tr>";
        }

        StringBuilder html = new StringBuilder();
        for (ExportOrderRequest.ProductInfo produto : produtos) {
            html.append("<tr>")
                .append("<td>").append(nullSafe(produto.produto())).append("</td>")
                .append("<td>").append(nullSafe(produto.volt())).append("</td>")
                .append("<td>").append(nullSafe(produto.serie())).append("</td>")
                .append("</tr>");
        }
        return html.toString();
    }

    private String generatePartsTableHtml(List<ExportOrderRequest.PartInfo> pecas) {
        if (pecas == null || pecas.isEmpty()) {
            return "<tr><td colspan=\"3\" style=\"text-align: center;\">Nenhuma peça aplicada</td></tr>";
        }

        StringBuilder html = new StringBuilder();
        for (ExportOrderRequest.PartInfo peca : pecas) {
            html.append("<tr>")
                .append("<td>").append(peca.quantidade() != null ? peca.quantidade() : 0).append("</td>")
                .append("<td>").append(nullSafe(peca.peca())).append("</td>")
                .append("<td>").append(nullSafe(peca.valor())).append("</td>")
                .append("</tr>");
        }
        return html.toString();
    }

    private String getCheckboxClass(Boolean checked) {
        return Boolean.TRUE.equals(checked) ? "checked" : "";
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
