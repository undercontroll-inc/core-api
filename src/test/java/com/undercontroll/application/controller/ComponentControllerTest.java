package com.undercontroll.application.controller;

import com.undercontroll.application.mapper.ComponentPartDtoMapperImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.gateway.TokenGateway;
import com.undercontroll.domain.usecase.component.*;
import com.undercontroll.infrastructure.config.SecurityConfig;
import com.undercontroll.application.dto.component.RegisterComponentRequest;
import com.undercontroll.application.dto.component.UpdateComponentRequest;
import com.undercontroll.application.controller.impl.ComponentController;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({SecurityConfig.class, ComponentPartDtoMapperImpl.class})
@AutoConfigureMockMvc
@WebMvcTest(ComponentController.class)
class ComponentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterComponentPort registerComponentPort;

    @MockitoBean
    private GetComponentsPort getComponentsPort;

    @MockitoBean
    private GetComponentByIdPort getComponentByIdPort;

    @MockitoBean
    private UpdateComponentPort updateComponentPort;

    @MockitoBean
    private DeleteComponentPort deleteComponentPort;

    // Required because AuthContextFilter depends on TokenPort
    @MockitoBean
    private TokenGateway tokenGateway;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("POST /v1/api/components - ADMINISTRATOR should create component and return 201")
    void administratorShouldCreateComponentSuccessfully() throws Exception {
        RegisterComponentRequest request = new RegisterComponentRequest(
                "Resistor", "10k Ohm resistor", "Brand A", "Electronics", 100, 1.50, "Supplier X"
        );

        ComponentOutput response = new ComponentOutput(1, "Resistor", "10k Ohm resistor", "Brand A", 1.50, 100L, "Supplier X", "Electronics");

        when(registerComponentPort.execute(any(RegisterComponentPort.Input.class))).thenReturn(response);

        mockMvc.perform(post("/v1/api/components")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.item").value("Resistor"))
                .andExpect(jsonPath("$.price").value(1.50))
                .andExpect(jsonPath("$.category").value("Electronics"));

        verify(registerComponentPort, times(1)).execute(any(RegisterComponentPort.Input.class));
    }

    @Test
    @DisplayName("POST /v1/api/components - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToCreateComponent() throws Exception {
        RegisterComponentRequest request = new RegisterComponentRequest(
                "Resistor", "10k Ohm resistor", "Brand A", "Electronics", 100, 1.50, "Supplier X"
        );

        mockMvc.perform(post("/v1/api/components")
                        .with(user("customer@example.com").roles("SCOPE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(registerComponentPort, never()).execute(any(RegisterComponentPort.Input.class));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("GET /v1/api/components - ADMINISTRATOR should get all components and return 200")
    void administratorShouldGetAllComponentsSuccessfully() throws Exception {
        ComponentOutput component1 = new ComponentOutput(1, "Resistor", "10k Ohm", "Brand A", 1.50, 100L, "Supplier X", "Electronics");
        ComponentOutput component2 = new ComponentOutput(2, "Capacitor", "100uF", "Brand B", 2.00, 50L, "Supplier Y", "Electronics");

        when(getComponentsPort.execute(null, null)).thenReturn(List.of(component1, component2));

        mockMvc.perform(get("/v1/api/components"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].item").value("Resistor"))
                .andExpect(jsonPath("$[1].item").value("Capacitor"))
                .andExpect(jsonPath("$.length()").value(2));

        verify(getComponentsPort, times(1)).execute(null, null);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("GET /v1/api/components - Should return 200 with empty list when no components found")
    void shouldReturn200WhenNoComponentsFound() throws Exception {
        when(getComponentsPort.execute(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/v1/api/components"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(getComponentsPort, times(1)).execute(null, null);
    }

    @Test
    @DisplayName("GET /v1/api/components - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToGetAllComponents() throws Exception {
        mockMvc.perform(get("/v1/api/components")
                        .with(user("customer@example.com").roles("SCOPE_CUSTOMER")))
                .andExpect(status().isForbidden());

        verify(getComponentsPort, never()).execute(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("GET /v1/api/components/{componentId} - Should get component by id and return 200")
    void shouldGetComponentByIdSuccessfully() throws Exception {
        ComponentOutput component = new ComponentOutput(1, "Resistor", "10k Ohm", "Brand A", 1.50, 100L, "Supplier X", "Electronics");

        when(getComponentByIdPort.execute(any(Integer.class)))
                .thenReturn(Optional.of(component));

        mockMvc.perform(get("/v1/api/components/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.item").value("Resistor"));

        verify(getComponentByIdPort, times(1)).execute(any(Integer.class));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("GET /v1/api/components?category=Electronics - Should get components by category and return 200")
    void shouldGetComponentsByCategorySuccessfully() throws Exception {
        ComponentOutput component1 = new ComponentOutput(1, "Resistor", "10k Ohm", "Brand A", 1.50, 100L, "Supplier X", "Electronics");
        ComponentOutput component2 = new ComponentOutput(2, "Capacitor", "100uF", "Brand B", 2.00, 50L, "Supplier Y", "Electronics");

        when(getComponentsPort.execute(eq("Electronics"), isNull()))
                .thenReturn(List.of(component1, component2));

        mockMvc.perform(get("/v1/api/components").param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Electronics"))
                .andExpect(jsonPath("$[1].category").value("Electronics"))
                .andExpect(jsonPath("$.length()").value(2));

        verify(getComponentsPort, times(1)).execute("Electronics", null);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("GET /v1/api/components?category=NonExistent - Should return 200 with empty list")
    void shouldReturn200WhenNoCategoryComponentsFound() throws Exception {
        when(getComponentsPort.execute(eq("NonExistent"), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/v1/api/components").param("category", "NonExistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(getComponentsPort, times(1)).execute("NonExistent", null);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("PUT /v1/api/components/{componentId} - ADMINISTRATOR should update component and return 200")
    void administratorShouldUpdateComponentSuccessfully() throws Exception {
        UpdateComponentRequest request = new UpdateComponentRequest(
                "Resistor Updated", "20k Ohm resistor", "Brand A", 1.75, "Supplier X", "Electronics"
        );

        ComponentOutput response = new ComponentOutput(
                1, "Resistor Updated", "20k Ohm resistor", "Brand A", 1.75, 100L, "Supplier X", "Electronics"
        );

        when(updateComponentPort.execute(anyInt(), any(UpdateComponentPort.Input.class))).thenReturn(response);

        mockMvc.perform(put("/v1/api/components/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item").value("Resistor Updated"))
                .andExpect(jsonPath("$.price").value(1.75));

        verify(updateComponentPort, times(1)).execute(anyInt(), any(UpdateComponentPort.Input.class));
    }

    @Test
    @DisplayName("PUT /v1/api/components/{componentId} - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToUpdateComponent() throws Exception {
        UpdateComponentRequest request = new UpdateComponentRequest(
                "Resistor Updated", "20k Ohm resistor", "Brand A", 1.75, "Supplier X", "Electronics"
        );

        mockMvc.perform(put("/v1/api/components/1")
                        .with(user("customer@example.com").roles("SCOPE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(updateComponentPort, never()).execute(anyInt(), any(UpdateComponentPort.Input.class));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("DELETE /v1/api/components/{componentId} - ADMINISTRATOR should delete component and return 204")
    void administratorShouldDeleteComponentSuccessfully() throws Exception {
        doNothing().when(deleteComponentPort).execute(any(Integer.class));

        mockMvc.perform(delete("/v1/api/components/1"))
                .andExpect(status().isNoContent());

        verify(deleteComponentPort, times(1)).execute(any(Integer.class));
    }

    @Test
    @DisplayName("DELETE /v1/api/components/{componentId} - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToDeleteComponent() throws Exception {
        mockMvc.perform(delete("/v1/api/components/1")
                        .with(user("customer@example.com").roles("SCOPE_CUSTOMER")))
                .andExpect(status().isForbidden());

        verify(deleteComponentPort, never()).execute(any(Integer.class));
    }
}
