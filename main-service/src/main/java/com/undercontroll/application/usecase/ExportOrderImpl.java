package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.ExportOrderPort;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.exception.OrderNotFoundException;
import com.undercontroll.domain.port.out.OrderRepositoryPort;
import com.undercontroll.domain.port.out.PdfExportPort;
import com.undercontroll.infrastructure.web.dto.ExportOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportOrderImpl implements ExportOrderPort {

    private final OrderRepositoryPort orderRepositoryPort;
    private final PdfExportPort pdfExportPort;

    @Override
    public Output execute(Input input) {
        log.info("Exporting order {} to PDF", input.orderId());

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        Order order = orderRepositoryPort.findById(input.orderId())
                .orElseThrow(() -> new OrderNotFoundException(
                        String.format("Order with id %d not found", input.orderId())));

        List<ExportOrderRequest.ProductInfo> produtos = order.getOrderItems().stream()
                .map(item -> new ExportOrderRequest.ProductInfo(
                        item.getType() + " " + item.getBrand() + " " + item.getModel(),
                        item.getVolt(),
                        item.getSeries()
                ))
                .toList();

        List<ExportOrderRequest.PartInfo> pecas = order.getDemands().stream()
                .map(part -> new ExportOrderRequest.PartInfo(
                        part.getQuantity(),
                        part.getComponent().getName(),
                        String.format("R$ %.2f", part.getComponent().getPrice() * part.getQuantity())
                ))
                .toList();

        ExportOrderRequest exportRequest = new ExportOrderRequest(
                order.getId().toString(),
                order.getId().toString(),
                order.getNf(),
                dtf.format(order.getReceived_at()),
                "Loja",
                produtos,
                order.getUser().getEmail(),
                order.getUser().getAddress(),
                order.getUser().getPhone(),
                dtf.format(order.getReceived_at()),
                pecas,
                String.format("R$ %.2f", order.calculateTotal()),
                dtf.format(order.getCompletedTime()),
                "Técnico",
                order.isFabricGuarantee(),
                false, // orcamento - você pode adicionar este campo ao Order se necessário
                order.isReturnGuarantee()
        );

        byte[] pdfData = pdfExportPort.exportOS(exportRequest);

        return new Output(pdfData);
    }
}
