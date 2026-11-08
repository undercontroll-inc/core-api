package com.undercontroll.application.controller.publicpage;

import com.undercontroll.application.controller.impl.PublicOrderRestController;
import com.undercontroll.application.mapper.OrderDtoMapperImpl;
import com.undercontroll.application.mapper.ComponentPartDtoMapperImpl;
import com.undercontroll.application.mapper.OrderItemDtoMapperImpl;
import com.undercontroll.application.mapper.UserDtoMapperImpl;
import com.undercontroll.domain.enums.ApprovalDecision;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.exception.TrackingNotFoundException;
import com.undercontroll.domain.gateway.TokenGateway;
import com.undercontroll.domain.model.ShopProfile;
import com.undercontroll.domain.usecase.order.DecidePublicOrderPort;
import com.undercontroll.domain.usecase.order.GetPublicOrderPort;
import com.undercontroll.domain.usecase.order.PublicOrderView;
import com.undercontroll.infrastructure.config.SecurityConfig;
import com.undercontroll.infrastructure.handler.OrderExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({
        SecurityConfig.class,
        OrderExceptionHandler.class,
        OrderDtoMapperImpl.class,
        UserDtoMapperImpl.class,
        OrderItemDtoMapperImpl.class,
        ComponentPartDtoMapperImpl.class,
        PublicOrderControllerTest.Shop.class
})
@AutoConfigureMockMvc
@WebMvcTest({PublicOrderController.class, PublicOrderRestController.class})
class PublicOrderControllerTest {

    static final String TOKEN = "7Hq2xLmP9vKa3TzR8dNw4c";

    static class Shop {
        @Bean
        ShopProfile shopProfile() {
            return new ShopProfile("Pelluci", "(11) 4000-1234", "5511999990000", "Rua A, 10", "Seg a sex, 9h às 18h",
                    90, ZoneId.of("America/Sao_Paulo"), "https://loja.example");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetPublicOrderPort getPublicOrderPort;

    @MockitoBean
    private DecidePublicOrderPort decidePublicOrderPort;

    @MockitoBean
    private TokenGateway tokenGateway;

    static PublicOrderView view(OrderStatus status, ApprovalDecision decision) {
        return new PublicOrderView(
                TOKEN, 123, "Maria",
                new PublicOrderView.Device("Liquidificador", "Arno", "LN50"),
                status,
                status == OrderStatus.AWAITING_APPROVAL ? "Aguardando sua aprovação" : "Em conserto",
                null,
                List.of(new PublicOrderView.TimelineEntry("Aparelho recebido", LocalDate.of(2026, 11, 3))),
                null,
                new PublicOrderView.Budget(150.0, 30.0, 0.0, 180.0, "Garantia de 90 dias no conserto"),
                decision,
                decision == null ? null : Instant.parse("2026-11-07T15:00:00Z"),
                status == OrderStatus.AWAITING_APPROVAL,
                new PublicOrderView.Shop("Pelluci", "(11) 4000-1234", "1140001234", "https://wa.me/5511999990000", "Rua A, 10", "Seg a sex, 9h às 18h"));
    }

    @Test
    @DisplayName("awaiting approval: shows budget, both buttons and contact links, with no login and safe headers")
    void awaitingApproval() throws Exception {
        when(getPublicOrderPort.execute(TOKEN)).thenReturn(view(OrderStatus.AWAITING_APPROVAL, null));

        MvcResult result = mockMvc.perform(get("/p/" + TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'none'")))
                .andExpect(content().string(containsString("Olá, Maria!")))
                .andExpect(content().string(containsString("Liquidificador Arno LN50")))
                .andExpect(content().string(containsString("R$ 180,00")))
                .andExpect(content().string(containsString("✅ Aprovar conserto")))
                .andExpect(content().string(containsString("❌ Não quero consertar")))
                .andExpect(content().string(containsString("/p/" + TOKEN + "?confirmar=APPROVED")))
                .andExpect(content().string(containsString("https://wa.me/5511999990000")))
                .andExpect(content().string(containsString("tel:1140001234")))
                .andExpect(content().string(not(containsString("<script"))))
                .andReturn();
        assertTrue(result.getResponse().getContentAsByteArray().length < 50_000);
    }

    @Test
    @DisplayName("confirmation step posts the chosen decision")
    void confirmation() throws Exception {
        when(getPublicOrderPort.execute(TOKEN)).thenReturn(view(OrderStatus.AWAITING_APPROVAL, null));

        mockMvc.perform(get("/p/" + TOKEN).param("confirmar", "DECLINED"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tem certeza?")))
                .andExpect(content().string(containsString("name=\"decision\" value=\"DECLINED\"")))
                .andExpect(content().string(containsString("Sim, não quero consertar")))
                .andExpect(content().string(not(containsString("✅ Aprovar conserto"))));
    }

    @Test
    @DisplayName("the decision form redirects with 303 back to the page, also when repeated")
    void decide() throws Exception {
        when(decidePublicOrderPort.execute(TOKEN, ApprovalDecision.APPROVED)).thenReturn(view(OrderStatus.IN_REPAIR, ApprovalDecision.APPROVED));

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/p/" + TOKEN + "/decision")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("decision", "APPROVED"))
                    .andExpect(status().isSeeOther())
                    .andExpect(redirectedUrl("/p/" + TOKEN));
        }
        verify(decidePublicOrderPort, org.mockito.Mockito.times(2)).execute(TOKEN, ApprovalDecision.APPROVED);
    }

    @Test
    @DisplayName("after the decision the buttons are gone and the recorded decision is shown")
    void decided() throws Exception {
        when(getPublicOrderPort.execute(TOKEN)).thenReturn(view(OrderStatus.IN_REPAIR, ApprovalDecision.APPROVED));

        mockMvc.perform(get("/p/" + TOKEN).param("confirmar", "DECLINED"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Conserto aprovado em 07/11/2026")))
                .andExpect(content().string(not(containsString("Tem certeza?"))))
                .andExpect(content().string(not(containsString("Aprovar conserto"))));
    }

    @Test
    @DisplayName("unknown or expired link shows the friendly 404 page")
    void notFound() throws Exception {
        when(getPublicOrderPort.execute("AAAAAAAAAAAAAAAAAAAAAA")).thenThrow(new TrackingNotFoundException());

        mockMvc.perform(get("/p/AAAAAAAAAAAAAAAAAAAAAA"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Link não encontrado.")))
                .andExpect(content().string(containsString("Fale com a loja.")));
    }

    @Test
    @DisplayName("public JSON returns the safe view and 404 TRACKING_NOT_FOUND for unknown links")
    void json() throws Exception {
        when(getPublicOrderPort.execute(TOKEN)).thenReturn(view(OrderStatus.AWAITING_APPROVAL, null));
        when(getPublicOrderPort.execute("AAAAAAAAAAAAAAAAAAAAAA")).thenThrow(new TrackingNotFoundException());

        mockMvc.perform(get("/v1/public/orders/" + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerFirstName").value("Maria"))
                .andExpect(jsonPath("$.budget.total").value(180.0))
                .andExpect(jsonPath("$.canDecide").value(true))
                .andExpect(jsonPath("$.shop.whatsappUrl").value("https://wa.me/5511999990000"))
                .andExpect(jsonPath("$.token").doesNotExist());
        mockMvc.perform(get("/v1/public/orders/AAAAAAAAAAAAAAAAAAAAAA"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRACKING_NOT_FOUND"));
    }

    @Test
    @DisplayName("the logo is served without login")
    void logo() throws Exception {
        mockMvc.perform(get("/p/assets/logo.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
        verify(getPublicOrderPort, never()).execute(org.mockito.ArgumentMatchers.anyString());
    }
}
