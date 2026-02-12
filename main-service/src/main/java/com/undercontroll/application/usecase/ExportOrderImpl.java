package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.ExportOrderPort;
import com.undercontroll.domain.entity.Order;
import com.undercontroll.domain.exception.OrderNotFoundException;
import com.undercontroll.infrastructure.persistence.repository.OrderJpaRepository;
import com.undercontroll.application.service.PdfExportService;
import com.undercontroll.infrastructure.web.dto.ExportOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportOrderImpl implements ExportOrderPort {

    private final OrderJpaRepository repository;
    private final PdfExportService pdfExportService;

    @Override
    public Output execute(Input input) {
        log.info("Exporting order {} to PDF", input.orderId());

        Order order = repository.findById(input.orderId())
                .orElseThrow(() -> new OrderNotFoundException(
                        String.format("Order with id %d not found", input.orderId())));

        // Note: This is a simplified version. You would need to enrich the order with 
        // the full data structure expected by PdfExportService.exportOS()
        // Implement the full logic from the original OrderService.exportPdf() method

        byte[] pdfData = new byte[0]; // Placeholder - implement full logic

        return new Output(pdfData);
    }
}
