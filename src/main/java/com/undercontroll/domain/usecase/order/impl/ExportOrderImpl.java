package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.gateway.StorageGateway;
import com.undercontroll.domain.usecase.order.ExportOrderPort;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.exception.OrderNotFoundException;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.gateway.PdfExportGateway;
import com.undercontroll.domain.model.ServiceOrderDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportOrderImpl implements ExportOrderPort {

    private final OrderGateway orderGateway;
    private final PdfExportGateway pdfExportGateway;
    private final StorageGateway storageGateway;

    @Override
    public byte[] execute(Integer orderId) {
        log.info("Exporting order {} to PDF", orderId);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        Order order = orderGateway.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        String.format("Order with id %d not found", orderId)));

        List<ServiceOrderDocument.ProductInfo> produtos = order.getOrderItems().stream()
                .map(item -> new ServiceOrderDocument.ProductInfo(
                        item.getType() + " " + item.getBrand() + " " + item.getModel(),
                        item.getVolt(),
                        item.getSeries()
                ))
                .toList();

        List<ServiceOrderDocument.PartInfo> pecas = order.getDemands().stream()
                .map(part -> new ServiceOrderDocument.PartInfo(
                        part.getQuantity(),
                        part.getComponent().getName(),
                        String.format("R$ %.2f", part.getComponent().getPrice() * part.getQuantity())
                ))
                .toList();

        final var receivedAt = order.getReceived_at() != null ? dtf.format(order.getReceived_at()) : null;
        final var completedAt = order.getCompletedTime() != null ? dtf.format(order.getCompletedTime()) : null;

        ServiceOrderDocument document = new ServiceOrderDocument(
                order.getId().toString(),
                order.getId().toString(),
                order.getNf(),
                receivedAt,
                "Loja",
                produtos,
                order.getUser().getEmail(),
                order.getUser().getAddress(),
                order.getUser().getPhone(),
                receivedAt,
                pecas,
                String.format("R$ %.2f", order.calculateTotal()),
                completedAt,
                "Técnico",
                order.isFabricGuarantee(),
                null,
                order.isReturnGuarantee()
        );

        byte[] pdfData = pdfExportGateway.exportOS(document);

        final var key = String.format(order.getId().toString(), order.getNf(), ".pdf");

        storageGateway.storeOrderExport(key, pdfData);

        return pdfData;
    }
}
