package com.undercontroll.application.controller;

import com.undercontroll.application.controller.impl.OrderLifecycleController;
import com.undercontroll.application.mapper.ComponentPartDtoMapperImpl;
import com.undercontroll.application.mapper.OrderDtoMapperImpl;
import com.undercontroll.application.mapper.OrderItemDtoMapperImpl;
import com.undercontroll.application.mapper.UserDtoMapperImpl;
import com.undercontroll.domain.enums.ApprovalDecision;
import com.undercontroll.domain.enums.OrderActor;
import com.undercontroll.domain.enums.OrderOutcome;
import com.undercontroll.domain.enums.OrderPhotoKind;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.exception.InvalidLabelReadingException;
import com.undercontroll.domain.exception.InvalidOrderTransitionException;
import com.undercontroll.domain.exception.LabelReadingUnavailableException;
import com.undercontroll.domain.exception.OrderBudgetEmptyException;
import com.undercontroll.domain.exception.OrderConcurrentUpdateException;
import com.undercontroll.domain.exception.OrderNotFoundException;
import com.undercontroll.domain.exception.OrderPhotoLimitException;
import com.undercontroll.domain.gateway.TokenGateway;
import com.undercontroll.domain.model.LabelReading;
import com.undercontroll.domain.model.OrderStatusEvent;
import com.undercontroll.domain.usecase.order.CompleteOrderPhotoUploadPort;
import com.undercontroll.domain.usecase.order.RequestLabelUploadPort;
import com.undercontroll.domain.usecase.order.RequestOrderPhotoUploadPort;
import com.undercontroll.domain.exception.InvalidUploadException;
import com.undercontroll.domain.storage.PresignedUploadUrl;
import com.undercontroll.domain.storage.UploadTicket;
import com.undercontroll.application.mapper.UploadDtoMapper;
import com.undercontroll.infrastructure.handler.StorageExceptionHandler;
import com.undercontroll.domain.usecase.order.CreateTrackingLinkPort;
import com.undercontroll.domain.usecase.order.DecideOrderBudgetPort;
import com.undercontroll.domain.usecase.order.DeleteOrderPhotoPort;
import com.undercontroll.domain.usecase.order.ListOrderPhotosPort;
import com.undercontroll.domain.usecase.order.LookupOrderByTokenPort;
import com.undercontroll.domain.usecase.order.OrderOutput;
import com.undercontroll.domain.usecase.order.OrderPhotoOutput;
import com.undercontroll.domain.usecase.order.ReadApplianceLabelPort;
import com.undercontroll.domain.usecase.order.SendOrderBudgetPort;
import com.undercontroll.domain.usecase.order.TransitionOrderPort;
import com.undercontroll.infrastructure.config.SecurityConfig;
import com.undercontroll.infrastructure.handler.OrderExceptionHandler;
import com.undercontroll.infrastructure.handler.TranscriptionExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({
        SecurityConfig.class,
        OrderExceptionHandler.class,
        TranscriptionExceptionHandler.class,
        StorageExceptionHandler.class,
        UploadDtoMapper.class,
        OrderDtoMapperImpl.class,
        UserDtoMapperImpl.class,
        OrderItemDtoMapperImpl.class,
        ComponentPartDtoMapperImpl.class
})
@AutoConfigureMockMvc
@WebMvcTest(OrderLifecycleController.class)
class OrderLifecycleControllerTest {

    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");
    private static final UUID UPLOAD = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private TransitionOrderPort transitionOrderPort;
    @MockitoBean private SendOrderBudgetPort sendOrderBudgetPort;
    @MockitoBean private DecideOrderBudgetPort decideOrderBudgetPort;
    @MockitoBean private CreateTrackingLinkPort createTrackingLinkPort;
    @MockitoBean private LookupOrderByTokenPort lookupOrderByTokenPort;
    @MockitoBean private ReadApplianceLabelPort readApplianceLabelPort;
    @MockitoBean private RequestOrderPhotoUploadPort requestOrderPhotoUploadPort;
    @MockitoBean private CompleteOrderPhotoUploadPort completeOrderPhotoUploadPort;
    @MockitoBean private RequestLabelUploadPort requestLabelUploadPort;
    @MockitoBean private ListOrderPhotosPort listOrderPhotosPort;
    @MockitoBean private DeleteOrderPhotoPort deleteOrderPhotoPort;
    @MockitoBean private TokenGateway tokenGateway;

    private static OrderOutput output(OrderStatus status, OrderOutcome outcome) {
        return new OrderOutput(1, null, new ArrayList<>(), new ArrayList<>(), 0.0, 180.0, 0.0, 180.0, "2026-11-03",
                "2026-11-10", null, true, null, null, status, null, outcome, "https://loja.example/p/7Hq2xLmP9vKa3TzR8dNw4c",
                NOW, 180.0, ApprovalDecision.APPROVED, NOW, OrderActor.SHOP, null, null,
                new ArrayList<>(List.of(new OrderStatusEvent(1L, 1, OrderStatus.AWAITING_APPROVAL, OrderStatus.IN_REPAIR, null, OrderActor.SHOP, "Aprovou por telefone", NOW))),
                new ArrayList<>(List.of(new OrderPhotoOutput(3L, OrderPhotoKind.DEVICE, "https://s3/a", NOW))));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("transition returns the order with history, photos and lifecycle fields")
    void transition() throws Exception {
        when(transitionOrderPort.execute(new TransitionOrderPort.Input(1, OrderStatus.IN_REPAIR, null, "ok")))
                .thenReturn(output(OrderStatus.IN_REPAIR, null));

        mockMvc.perform(post("/v1/api/orders/1/transitions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":\"IN_REPAIR\",\"note\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REPAIR"))
                .andExpect(jsonPath("$.approvalDecision").value("APPROVED"))
                .andExpect(jsonPath("$.decidedVia").value("SHOP"))
                .andExpect(jsonPath("$.trackingUrl").value("https://loja.example/p/7Hq2xLmP9vKa3TzR8dNw4c"))
                .andExpect(jsonPath("$.deadline").value("2026-11-10"))
                .andExpect(jsonPath("$.statusHistory[0].to").value("IN_REPAIR"))
                .andExpect(jsonPath("$.statusHistory[0].actor").value("SHOP"))
                .andExpect(jsonPath("$.photos[0].kind").value("DEVICE"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("lifecycle errors map to their codes")
    void errors() throws Exception {
        when(transitionOrderPort.execute(any())).thenThrow(new InvalidOrderTransitionException("Não é possível"));
        mockMvc.perform(post("/v1/api/orders/1/transitions").contentType(MediaType.APPLICATION_JSON).content("{\"to\":\"DELIVERED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_INVALID_TRANSITION"));

        when(decideOrderBudgetPort.execute(any())).thenThrow(new OrderConcurrentUpdateException(1, null));
        mockMvc.perform(post("/v1/api/orders/1/budget/decision").contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_CONCURRENT_UPDATE"));

        when(sendOrderBudgetPort.execute(1)).thenThrow(new OrderBudgetEmptyException());
        mockMvc.perform(post("/v1/api/orders/1/budget/send"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ORDER_BUDGET_EMPTY"));

        when(lookupOrderByTokenPort.execute("x")).thenThrow(new OrderNotFoundException("nope"));
        mockMvc.perform(get("/v1/api/orders/lookup").param("token", "x"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("budget send and tracking link return the WhatsApp text; lookup returns the id")
    void links() throws Exception {
        when(sendOrderBudgetPort.execute(1)).thenReturn(new SendOrderBudgetPort.Output(
                "https://loja.example/p/7Hq2xLmP9vKa3TzR8dNw4c", "Olá, Maria! ...", output(OrderStatus.AWAITING_APPROVAL, null)));
        when(createTrackingLinkPort.execute(1)).thenReturn(new CreateTrackingLinkPort.Output("https://loja.example/p/x", "Olá!"));
        when(lookupOrderByTokenPort.execute("7Hq2xLmP9vKa3TzR8dNw4c")).thenReturn(1);

        mockMvc.perform(post("/v1/api/orders/1/budget/send"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whatsappText").value("Olá, Maria! ..."))
                .andExpect(jsonPath("$.order.status").value("AWAITING_APPROVAL"));
        mockMvc.perform(post("/v1/api/orders/1/tracking-link"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingUrl").value("https://loja.example/p/x"));
        mockMvc.perform(get("/v1/api/orders/lookup").param("token", "7Hq2xLmP9vKa3TzR8dNw4c"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    private static UploadTicket ticket(String key) {
        return new UploadTicket(UPLOAD, new PresignedUploadUrl("http://minio:9000/undercontroll-uploads/" + key + "?X-Amz-Signature=x",
                "PUT", Map.of("Content-Type", "image/jpeg", "Content-Length", "183442"), key, NOW.plusSeconds(300)));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("label reading: upload ticket, then 200 with fields, 400 invalid, 503 unavailable")
    void labelReading() throws Exception {
        when(requestLabelUploadPort.execute("image/jpeg", 183442L)).thenReturn(ticket("label-scans/7/a.jpg"));
        when(readApplianceLabelPort.execute(UPLOAD))
                .thenReturn(new LabelReading(true, "liquidificador", "Arno", "LN50", "SN123", "127V", 0.92))
                .thenThrow(new InvalidLabelReadingException("Upload não encontrado"))
                .thenThrow(new LabelReadingUnavailableException());
        String body = "{\"uploadId\":\"" + UPLOAD + "\"}";

        mockMvc.perform(post("/v1/api/orders/label-reading/uploads").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\"image/jpeg\",\"sizeBytes\":183442}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uploadId").value(UPLOAD.toString()))
                .andExpect(jsonPath("$.method").value("PUT"))
                .andExpect(jsonPath("$.headers['Content-Length']").value("183442"));
        mockMvc.perform(post("/v1/api/orders/label-reading").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isLabel").value(true))
                .andExpect(jsonPath("$.voltage").value("127V"));
        mockMvc.perform(post("/v1/api/orders/label-reading").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("LABEL_READING_INVALID"));
        mockMvc.perform(post("/v1/api/orders/label-reading").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("LABEL_READING_UNAVAILABLE"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("photos: upload ticket 201, limit 409, complete 201, invalid upload 400, list and delete")
    void photos() throws Exception {
        when(requestOrderPhotoUploadPort.execute(new RequestOrderPhotoUploadPort.Input(1, OrderPhotoKind.DEVICE, "image/jpeg", 183442L)))
                .thenReturn(ticket("order-photos/1/a.jpg"))
                .thenThrow(new OrderPhotoLimitException(6));
        when(completeOrderPhotoUploadPort.execute(1, UPLOAD))
                .thenReturn(new OrderPhotoOutput(3L, OrderPhotoKind.DEVICE, "https://minio/a", NOW))
                .thenThrow(new InvalidUploadException("O conteúdo do arquivo não confere com o tipo informado"));
        when(listOrderPhotosPort.execute(1)).thenReturn(List.of(new OrderPhotoOutput(3L, OrderPhotoKind.DEVICE, "https://minio/a", NOW)));
        String request = "{\"kind\":\"DEVICE\",\"contentType\":\"image/jpeg\",\"sizeBytes\":183442}";

        mockMvc.perform(post("/v1/api/orders/1/photos/uploads").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uploadUrl").value(org.hamcrest.Matchers.startsWith("http://minio:9000/undercontroll-uploads/order-photos/1/")));
        mockMvc.perform(post("/v1/api/orders/1/photos/uploads").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_PHOTO_LIMIT"));
        mockMvc.perform(post("/v1/api/orders/1/photos/uploads/" + UPLOAD + "/complete"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value("https://minio/a"));
        mockMvc.perform(post("/v1/api/orders/1/photos/uploads/" + UPLOAD + "/complete"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UPLOAD_INVALID"));
        mockMvc.perform(get("/v1/api/orders/1/photos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3));
        mockMvc.perform(delete("/v1/api/orders/1/photos/3")).andExpect(status().isNoContent());
        verify(deleteOrderPhotoPort).execute(1, 3L);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("customers cannot use any lifecycle route")
    void customerForbidden() throws Exception {
        mockMvc.perform(post("/v1/api/orders/1/transitions").contentType(MediaType.APPLICATION_JSON).content("{\"to\":\"IN_REPAIR\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/api/orders/1/budget/send")).andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/api/orders/1/budget/decision").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/v1/api/orders/lookup").param("token", "x")).andExpect(status().isForbidden());
        mockMvc.perform(get("/v1/api/orders/1/photos")).andExpect(status().isForbidden());
        verify(transitionOrderPort, never()).execute(any());
    }
}
