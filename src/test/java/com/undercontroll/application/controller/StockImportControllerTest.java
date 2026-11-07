package com.undercontroll.application.controller;

import com.undercontroll.application.controller.impl.StockImportController;
import com.undercontroll.application.mapper.StockImportDtoMapper;
import com.undercontroll.domain.enums.StockImportItemAction;
import com.undercontroll.domain.enums.StockImportMatchType;
import com.undercontroll.domain.enums.StockImportSourceType;
import com.undercontroll.domain.enums.StockImportStage;
import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.domain.exception.FieldViolation;
import com.undercontroll.domain.exception.InvalidStockImportException;
import com.undercontroll.domain.exception.StockImportComponentNotFoundException;
import com.undercontroll.domain.exception.StockImportInvalidStateException;
import com.undercontroll.domain.exception.StockImportNotFoundException;
import com.undercontroll.domain.exception.StockImportUnavailableException;
import com.undercontroll.domain.exception.StockImportValidationException;
import com.undercontroll.domain.gateway.TokenGateway;
import com.undercontroll.domain.model.stockimport.MatchCandidate;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.model.stockimport.StockImportItem;
import com.undercontroll.domain.model.stockimport.StockImportReview;
import com.undercontroll.domain.usecase.stockimport.CancelStockImportPort;
import com.undercontroll.domain.usecase.stockimport.ConfirmStockImportPort;
import com.undercontroll.domain.usecase.stockimport.CreateStockImportPort;
import com.undercontroll.domain.usecase.stockimport.GetStockImportPort;
import com.undercontroll.domain.usecase.stockimport.ListStockImportsPort;
import com.undercontroll.domain.usecase.stockimport.RetryStockImportPort;
import com.undercontroll.domain.usecase.stockimport.UpdateStockImportPort;
import com.undercontroll.infrastructure.config.SecurityConfig;
import com.undercontroll.infrastructure.handler.GlobalExceptionHandler;
import com.undercontroll.infrastructure.handler.StockImportExceptionHandler;
import com.undercontroll.infrastructure.handler.TranscriptionExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({
        SecurityConfig.class,
        StockImportExceptionHandler.class,
        TranscriptionExceptionHandler.class,
        GlobalExceptionHandler.class,
        StockImportDtoMapper.class
})
@AutoConfigureMockMvc
@WebMvcTest(StockImportController.class)
class StockImportControllerTest {

    private static final String BASE = "/v1/api/stock-imports";
    private static final String REVIEW = """
            {
              "supplier": {"name": "Eletro Pecas", "cnpj": "12345678000195"},
              "invoice": {"number": "1234", "series": "1", "issuedAt": "2026-11-03T10:15:00-03:00"},
              "items": [
                {"id": 10, "description": "Resistor 10k", "quantity": 100, "unit": "UN", "unitPrice": 0.15,
                 "action": "ADD_TO_EXISTING", "componentId": 5, "updatePrice": true},
                {"id": null, "description": "Diodo", "quantity": 4, "action": "CREATE_NEW",
                 "newComponent": {"name": "Diodo 1N4007", "brand": null, "category": "Eletronicos", "description": null}}
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateStockImportPort createStockImportPort;
    @MockitoBean
    private ListStockImportsPort listStockImportsPort;
    @MockitoBean
    private GetStockImportPort getStockImportPort;
    @MockitoBean
    private UpdateStockImportPort updateStockImportPort;
    @MockitoBean
    private ConfirmStockImportPort confirmStockImportPort;
    @MockitoBean
    private RetryStockImportPort retryStockImportPort;
    @MockitoBean
    private CancelStockImportPort cancelStockImportPort;
    @MockitoBean
    private TokenGateway tokenGateway;

    private static MockMultipartFile file() {
        return new MockMultipartFile("file", "nota.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0});
    }

    private static StockImport ready() {
        return StockImport.builder()
                .id(1L)
                .status(StockImportStatus.READY)
                .sourceType(StockImportSourceType.IMAGE)
                .supplierName("Eletro Pecas")
                .total(new BigDecimal("15.00"))
                .itemCount(1)
                .items(List.of(StockImportItem.builder()
                        .id(10L)
                        .lineNumber(1)
                        .description("Resistor 10k")
                        .quantity(new BigDecimal("100"))
                        .unitPrice(new BigDecimal("0.15"))
                        .total(new BigDecimal("15.00"))
                        .confidence(0.9)
                        .matchType(StockImportMatchType.FUZZY)
                        .matchedComponentId(5)
                        .matchConfidence(0.8)
                        .candidates(List.of(new MatchCandidate(5, "Resistor 10k Ohm", "Vishay", 30L, 0.2, 0.8)))
                        .action(StockImportItemAction.ADD_TO_EXISTING)
                        .componentId(5)
                        .build()))
                .createdAt(Instant.parse("2026-11-06T12:00:00Z"))
                .updatedAt(Instant.parse("2026-11-06T12:00:00Z"))
                .build();
    }

    @Test
    @WithMockUser(username = "12", roles = "ADMINISTRATOR")
    @DisplayName("POST returns 202 with PROCESSING and stage")
    void create() throws Exception {
        when(createStockImportPort.execute(any())).thenReturn(StockImport.builder()
                .id(1L).status(StockImportStatus.PROCESSING).stage(StockImportStage.RECEIVED)
                .sourceType(StockImportSourceType.IMAGE).build());

        mockMvc.perform(multipart(BASE).file(file()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.stage").value("RECEIVED"))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("customers are forbidden")
    void customerForbidden() throws Exception {
        mockMvc.perform(multipart(BASE).file(file())).andExpect(status().isForbidden());
        mockMvc.perform(get(BASE)).andExpect(status().isForbidden());
        verify(createStockImportPort, never()).execute(any());
    }

    @Test
    @DisplayName("unauthenticated is 401")
    void unauthenticated() throws Exception {
        mockMvc.perform(get(BASE + "/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("missing file part is 400 STOCK_IMPORT_INVALID")
    void missingFile() throws Exception {
        mockMvc.perform(multipart(BASE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STOCK_IMPORT_INVALID"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("invalid upload and disabled AI map to their codes")
    void uploadErrors() throws Exception {
        when(createStockImportPort.execute(any())).thenThrow(new InvalidStockImportException("File exceeds 10MB"));
        mockMvc.perform(multipart(BASE).file(file()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STOCK_IMPORT_INVALID"));

        doThrow(new StockImportUnavailableException()).when(createStockImportPort).execute(any());
        mockMvc.perform(multipart(BASE).file(file()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("STOCK_IMPORT_UNAVAILABLE"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("GET returns the import with nested supplier, invoice, match and item choices")
    void getImport() throws Exception {
        when(getStockImportPort.execute(1L)).thenReturn(ready());

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.supplier.name").value("Eletro Pecas"))
                .andExpect(jsonPath("$.invoice.total").value(15.00))
                .andExpect(jsonPath("$.itemsTotal").value(15.00))
                .andExpect(jsonPath("$.items[0].match.type").value("FUZZY"))
                .andExpect(jsonPath("$.items[0].match.componentId").value(5))
                .andExpect(jsonPath("$.items[0].match.candidates[0].name").value("Resistor 10k Ohm"))
                .andExpect(jsonPath("$.items[0].action").value("ADD_TO_EXISTING"))
                .andExpect(jsonPath("$.items[0].componentId").value(5))
                .andExpect(jsonPath("$.items[0].updatePrice").value(false))
                .andExpect(jsonPath("$.createdAt").value("2026-11-06T12:00:00Z"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("GET unknown is 404 STOCK_IMPORT_NOT_FOUND; bad id is 400")
    void notFound() throws Exception {
        when(getStockImportPort.execute(9L)).thenThrow(new StockImportNotFoundException(9L));

        mockMvc.perform(get(BASE + "/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STOCK_IMPORT_NOT_FOUND"));
        mockMvc.perform(get(BASE + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("list passes status and limit and returns summaries")
    void list() throws Exception {
        when(listStockImportsPort.execute(StockImportStatus.READY, 5)).thenReturn(List.of(ready()));

        mockMvc.perform(get(BASE).param("status", "READY").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].supplierName").value("Eletro Pecas"))
                .andExpect(jsonPath("$[0].itemCount").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("PUT maps the review request to the domain review")
    void update() throws Exception {
        when(updateStockImportPort.execute(eq(1L), any())).thenReturn(ready());

        mockMvc.perform(put(BASE + "/1").contentType(MediaType.APPLICATION_JSON).content(REVIEW))
                .andExpect(status().isOk());

        ArgumentCaptor<StockImportReview> captor = ArgumentCaptor.forClass(StockImportReview.class);
        verify(updateStockImportPort).execute(eq(1L), captor.capture());
        StockImportReview review = captor.getValue();
        assertEquals("12345678000195", review.supplierCnpj());
        assertEquals("1234", review.invoiceNumber());
        assertEquals(2, review.items().size());
        assertTrue(review.items().get(0).updatePrice());
        assertEquals(5, review.items().get(0).componentId());
        assertEquals("Diodo 1N4007", review.items().get(1).newComponent().name());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("confirm errors carry their codes and field errors")
    void confirmErrors() throws Exception {
        when(confirmStockImportPort.execute(eq(1L), any()))
                .thenThrow(new StockImportValidationException(List.of(new FieldViolation("items[0].quantity", "quantity must be a whole number"))));
        mockMvc.perform(post(BASE + "/1/confirm").contentType(MediaType.APPLICATION_JSON).content(REVIEW))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("items[0].quantity"));

        doThrow(new StockImportComponentNotFoundException(List.of(new FieldViolation("items[0].componentId", "component 5 does not exist"))))
                .when(confirmStockImportPort).execute(eq(1L), any());
        mockMvc.perform(post(BASE + "/1/confirm").contentType(MediaType.APPLICATION_JSON).content(REVIEW))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("COMPONENT_NOT_FOUND"))
                .andExpect(jsonPath("$.errors[0].field").value("items[0].componentId"));

        doThrow(new StockImportInvalidStateException(1L, StockImportStatus.CONFIRMED, "confirmed"))
                .when(confirmStockImportPort).execute(eq(1L), any());
        mockMvc.perform(post(BASE + "/1/confirm").contentType(MediaType.APPLICATION_JSON).content(REVIEW))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STOCK_IMPORT_INVALID_STATE"));

        mockMvc.perform(post(BASE + "/1/confirm").contentType(MediaType.APPLICATION_JSON).content("{\"items\": [{\"action\": \"EXPLODE\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("retry is 202 and cancel is 204, 409 when confirmed")
    void retryAndCancel() throws Exception {
        when(retryStockImportPort.execute(1L)).thenReturn(ready().toBuilder().status(StockImportStatus.PROCESSING).build());
        mockMvc.perform(post(BASE + "/1/retry"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        mockMvc.perform(delete(BASE + "/1")).andExpect(status().isNoContent());

        doThrow(new StockImportInvalidStateException(2L, StockImportStatus.CONFIRMED, "canceled"))
                .when(cancelStockImportPort).execute(2L);
        mockMvc.perform(delete(BASE + "/2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STOCK_IMPORT_INVALID_STATE"));
    }
}
