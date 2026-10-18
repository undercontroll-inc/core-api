package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.exception.OrderNotFoundException;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.gateway.PdfExportGateway;
import com.undercontroll.domain.gateway.StorageGateway;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.model.ServiceOrderDocument;
import com.undercontroll.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportOrderImplTest {

    @Mock
    private OrderGateway orderGateway;
    @Mock
    private PdfExportGateway pdfExportGateway;
    @Mock
    private StorageGateway storageGateway;

    @InjectMocks
    private ExportOrderImpl useCase;

    @Test
    @DisplayName("builds the service order document, renders the PDF and stores it")
    void exports() {
        Order order = Order.builder()
                .id(12)
                .nf("NF-1")
                .discount(0.0)
                .user(User.builder().email("ana@x.com").address("Rua 1").phone("1199").build())
                .orderItems(new ArrayList<>(List.of(OrderItem.builder().type("TV").brand("LG").model("X1").volt("220V").series("SN1").laborValue(50.0).build())))
                .demands(new ArrayList<>(List.of(Demand.builder().quantity(2L).component(ComponentPart.builder().name("Capacitor").price(2.5).build()).build())))
                .received_at(LocalDate.of(2026, 1, 10))
                .fabricGuarantee(true)
                .build();
        byte[] pdf = {1, 2, 3};
        when(orderGateway.findById(12)).thenReturn(Optional.of(order));
        when(pdfExportGateway.exportOS(any())).thenReturn(pdf);

        assertArrayEquals(pdf, useCase.execute(12));

        ArgumentCaptor<ServiceOrderDocument> document = ArgumentCaptor.forClass(ServiceOrderDocument.class);
        verify(pdfExportGateway).exportOS(document.capture());
        assertEquals("12", document.getValue().numeroOs());
        assertEquals("10/01/2026", document.getValue().data());
        assertNull(document.getValue().dataConserto());
        assertEquals("TV LG X1", document.getValue().produtos().get(0).produto());
        assertEquals("Capacitor", document.getValue().pecas().get(0).peca());
        assertEquals(String.format("R$ %.2f", 5.0), document.getValue().pecas().get(0).valor());
        assertEquals("ana@x.com", document.getValue().nome());
        verify(storageGateway).storeOrderExport("12-NF-1.pdf", pdf);
    }

    @Test
    @DisplayName("export key uses the order id, a safe NF and the pdf extension")
    void exportKey() {
        assertEquals("7.pdf", ExportOrderImpl.exportKey(Order.builder().id(7).build()));
        assertEquals("7.pdf", ExportOrderImpl.exportKey(Order.builder().id(7).nf(" ").build()));
        assertEquals("7-NF_12_A.pdf", ExportOrderImpl.exportKey(Order.builder().id(7).nf("NF 12/A").build()));
    }

    @Test
    @DisplayName("fails for an unknown order")
    void unknownOrder() {
        when(orderGateway.findById(99)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> useCase.execute(99));
    }
}
