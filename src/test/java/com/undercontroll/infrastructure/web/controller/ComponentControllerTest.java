package com.undercontroll.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.application.dto.ComponentDto;
import com.undercontroll.application.usecase.component.*;
import com.undercontroll.application.port.TokenPort;
import com.undercontroll.infrastructure.config.SecurityConfig;
import com.undercontroll.infrastructure.config.RateLimitProperties;
import com.undercontroll.presentation.dto.RegisterComponentRequest;
import com.undercontroll.presentation.dto.UpdateComponentRequest;
import com.undercontroll.presentation.controller.impl.ComponentController;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({SecurityConfig.class, RateLimitProperties.class})
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
    private GetComponentsByCategoryPort getComponentsByCategoryPort;

    @MockitoBean
    private GetComponentsByNamePort getComponentsByNamePort;

    @MockitoBean
    private UpdateComponentPort updateComponentPort;

    @MockitoBean
    private DeleteComponentPort deleteComponentPort;

    // Required because AuthContextFilter depends on TokenPort
    @MockitoBean
    private TokenPort tokenPort;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("POST /v1/api/components - ADMINISTRATOR should create component and return 201")
    void administratorShouldCreateComponentSuccessfully() throws Exception {
        RegisterComponentRequest request = new RegisterComponentRequest(
                "Resistor", "10k Ohm resistor", "Brand A", "Electronics", 100, 1.50, "Supplier X"
        );

        RegisterComponentPort.Output output = new RegisterComponentPort.Output(
                "Resistor", "10k Ohm resistor", "Brand A", 1.50, "Supplier X", "Electronics"
        );

        when(registerComponentPort.execute(any(RegisterComponentPort.Input.class))).thenReturn(output);

        mockMvc.perform(post("/v1/api/components")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Resistor"))
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
        ComponentDto component1 = new ComponentDto(1, "Resistor", "10k Ohm", "Brand A", 1.50, 100L, "Supplier X", "Electronics");
        ComponentDto component2 = new ComponentDto(2, "Capacitor", "100uF", "Brand B", 2.00, 50L, "Supplier Y", "Electronics");

        when(getComponentsPort.execute()).thenReturn(new GetComponentsPort.Output(List.of(component1, component2)));

        mockMvc.perform(get("/v1/api/components"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].item").value("Resistor"))
                .andExpect(jsonPath("$[1].item").value("Capacitor"))
                .andExpect(jsonPath("$.length()").value(2));

        verify(getComponentsPort, times(1)).execute();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("GET /v1/api/components - Should return 204 when no components found")
    void shouldReturn204WhenNoComponentsFound() throws Exception {
        when(getComponentsPort.execute()).thenReturn(new GetComponentsPort.Output(List.of()));

        mockMvc.perform(get("/v1/api/components"))
                .andExpect(status().isNoContent());

        verify(getComponentsPort, times(1)).execute();
    }

    @Test
    @DisplayName("GET /v1/api/components - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToGetAllComponents() throws Exception {
        mockMvc.perform(get("/v1/api/components")
                        .with(user("customer@example.com").roles("SCOPE_CUSTOMER")))
                .andExpect(status().isForbidden());

        verify(getComponentsPort, never()).execute();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("GET /v1/api/components/{componentId} - Should get component by id and return 200")
    void shouldGetComponentByIdSuccessfully() throws Exception {
        ComponentDto component = new ComponentDto(1, "Resistor", "10k Ohm", "Brand A", 1.50, 100L, "Supplier X", "Electronics");

        when(getComponentByIdPort.execute(any(GetComponentByIdPort.Input.class)))
                .thenReturn(new GetComponentByIdPort.Output(component));

        mockMvc.perform(get("/v1/api/components/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.item").value("Resistor"));

        verify(getComponentByIdPort, times(1)).execute(any(GetComponentByIdPort.Input.class));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("GET /v1/api/components/category/{category} - Should get components by category and return 200")
    void shouldGetComponentsByCategorySuccessfully() throws Exception {
        ComponentDto component1 = new ComponentDto(1, "Resistor", "10k Ohm", "Brand A", 1.50, 100L, "Supplier X", "Electronics");
        ComponentDto component2 = new ComponentDto(2, "Capacitor", "100uF", "Brand B", 2.00, 50L, "Supplier Y", "Electronics");

        when(getComponentsByCategoryPort.execute(any(GetComponentsByCategoryPort.Input.class)))
                .thenReturn(new GetComponentsByCategoryPort.Output(List.of(component1, component2)));

        mockMvc.perform(get("/v1/api/components/category/Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Electronics"))
                .andExpect(jsonPath("$[1].category").value("Electronics"))
                .andExpect(jsonPath("$.length()").value(2));

        verify(getComponentsByCategoryPort, times(1)).execute(any(GetComponentsByCategoryPort.Input.class));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("GET /v1/api/components/category/{category} - Should return 204 when no components in category")
    void shouldReturn204WhenNoCategoryComponentsFound() throws Exception {
        when(getComponentsByCategoryPort.execute(any(GetComponentsByCategoryPort.Input.class)))
                .thenReturn(new GetComponentsByCategoryPort.Output(List.of()));

        mockMvc.perform(get("/v1/api/components/category/NonExistent"))
                .andExpect(status().isNoContent());

        verify(getComponentsByCategoryPort, times(1)).execute(any(GetComponentsByCategoryPort.Input.class));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("PUT /v1/api/components/{componentId} - ADMINISTRATOR should update component and return 200")
    void administratorShouldUpdateComponentSuccessfully() throws Exception {
        UpdateComponentRequest request = new UpdateComponentRequest(
                "Resistor Updated", "20k Ohm resistor", "Brand A", 1.75, "Supplier X", "Electronics"
        );

        UpdateComponentPort.Output output = new UpdateComponentPort.Output(
                1, "Resistor Updated", "20k Ohm resistor", "Brand A", 1.75, 100L, "Supplier X", "Electronics"
        );

        when(updateComponentPort.execute(any(UpdateComponentPort.Input.class))).thenReturn(output);

        mockMvc.perform(put("/v1/api/components/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item").value("Resistor Updated"))
                .andExpect(jsonPath("$.price").value(1.75));

        verify(updateComponentPort, times(1)).execute(any(UpdateComponentPort.Input.class));
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

        verify(updateComponentPort, never()).execute(any(UpdateComponentPort.Input.class));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("DELETE /v1/api/components/{componentId} - ADMINISTRATOR should delete component and return 200")
    void administratorShouldDeleteComponentSuccessfully() throws Exception {
        when(deleteComponentPort.execute(any(DeleteComponentPort.Input.class)))
                .thenReturn(new DeleteComponentPort.Output(true, "Deleted"));

        mockMvc.perform(delete("/v1/api/components/1"))
                .andExpect(status().isOk());

        verify(deleteComponentPort, times(1)).execute(any(DeleteComponentPort.Input.class));
    }

    @Test
    @DisplayName("DELETE /v1/api/components/{componentId} - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToDeleteComponent() throws Exception {
        mockMvc.perform(delete("/v1/api/components/1")
                        .with(user("customer@example.com").roles("SCOPE_CUSTOMER")))
                .andExpect(status().isForbidden());

        verify(deleteComponentPort, never()).execute(any(DeleteComponentPort.Input.class));
    }
}
