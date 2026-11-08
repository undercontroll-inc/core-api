package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.usecase.order.ExportOrderPort;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.ShopProfile;
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

    private final OrderAccess orderAccess;
    private final ShopProfile shopProfile;
    private final PdfExportGateway pdfExportGateway;
    private final ObjectStorageGateway storageGateway;

    @Override
    public byte[] execute(Integer orderId) {
        log.info("Exporting order {} to PDF", orderId);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        Order order = orderAccess.requireVisible(orderId);

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
        final var completedAt = order.getCompletedAt() != null
                ? dtf.format(order.getCompletedAt().atZone(shopProfile.zone()))
                : order.getDeadline() != null ? dtf.format(order.getDeadline()) : null;

        ServiceOrderDocument document = new ServiceOrderDocument(
                order.getId().toString(),
                order.getId().toString(),
                order.getNf(),
                receivedAt,
                shopProfile.name(),
                produtos,
                order.getUser().getEmail(),
                order.getUser().getAddress(),
                order.getUser().getPhone(),
                receivedAt,
                pecas,
                String.format("R$ %.2f", order.calculateTotal()),
                completedAt,
                shopProfile.name(),
                order.isFabricGuarantee(),
                null,
                order.isReturnGuarantee()
        );

        byte[] pdfData = pdfExportGateway.exportOS(document);

        storageGateway.put(StorageArea.ORDER_EXPORTS, exportKey(order), pdfData, "application/pdf");

        return pdfData;
    }

    static String exportKey(Order order) {
        String nf = order.getNf();
        String prefix = StorageArea.ORDER_EXPORTS.policy().prefix() + "/";
        if (nf == null || nf.isBlank()) {
            return prefix + order.getId() + ".pdf";
        }
        return prefix + order.getId() + "-" + nf.trim().replaceAll("[^A-Za-z0-9_-]", "_") + ".pdf";
    }
}
