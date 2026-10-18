package com.undercontroll.domain.usecase.component.impl;

import com.undercontroll.domain.exception.ComponentNotFoundException;
import com.undercontroll.domain.exception.InvalidComponentCreationException;
import com.undercontroll.domain.exception.InvalidDeleteComponentException;
import com.undercontroll.domain.exception.InvalidUpdateComponentException;
import com.undercontroll.domain.gateway.ComponentGateway;
import com.undercontroll.domain.gateway.MetricsGateway;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.usecase.component.ComponentOutput;
import com.undercontroll.domain.usecase.component.RegisterComponentPort;
import com.undercontroll.domain.usecase.component.UpdateComponentPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponentUseCasesTest {

    @Mock
    private ComponentGateway componentGateway;

    @Mock
    private MetricsGateway metricsGateway;

    private static ComponentPart part(int id, String name) {
        return ComponentPart.builder().id(id).name(name).brand("Epcos").price(2.5).quantity(10L).category("Elétrica").build();
    }

    @Test
    @DisplayName("register maps the item name and defaults quantity to zero")
    void register() {
        when(componentGateway.save(any(ComponentPart.class))).thenAnswer(invocation -> {
            ComponentPart saved = invocation.getArgument(0);
            saved.setId(7);
            return saved;
        });

        ComponentOutput output = new RegisterComponentImpl(componentGateway, metricsGateway).execute(
                new RegisterComponentPort.Input("Capacitor", "45uF", "Epcos", "Elétrica", null, 2.5, "Loja")
        );

        assertEquals(new ComponentOutput(7, "Capacitor", "45uF", "Epcos", 2.5, 0L, "Loja", "Elétrica"), output);
        verify(metricsGateway).incrementComponentCreated();
    }

    @Test
    @DisplayName("register rejects incomplete data")
    void registerInvalid() {
        RegisterComponentImpl useCase = new RegisterComponentImpl(componentGateway, metricsGateway);
        RegisterComponentPort.Input input = new RegisterComponentPort.Input("Capacitor", "45uF", "Epcos", "Elétrica", 1, 0.0, "Loja");

        assertThrows(InvalidComponentCreationException.class, () -> useCase.execute(input));
        verify(componentGateway, never()).save(any());
    }

    @Test
    @DisplayName("update only overwrites provided non-blank fields")
    void update() {
        ComponentPart existing = part(3, "Capacitor");
        when(componentGateway.findById(3)).thenReturn(Optional.of(existing));
        when(componentGateway.save(existing)).thenReturn(existing);

        ComponentOutput output = new UpdateComponentImpl(componentGateway, metricsGateway).execute(
                3, new UpdateComponentPort.Input("Capacitor 50uF", "", null, 3.0, "Nova loja", "Motor")
        );

        assertEquals("Capacitor 50uF", output.item());
        assertEquals("Epcos", output.brand());
        assertEquals(3.0, output.price());
        assertEquals("Nova loja", output.supplier());
        assertEquals("Motor", output.category());
        verify(metricsGateway).incrementComponentUpdated();
    }

    @Test
    @DisplayName("update validates the id and existence")
    void updateInvalid() {
        UpdateComponentImpl useCase = new UpdateComponentImpl(componentGateway, metricsGateway);
        UpdateComponentPort.Input input = new UpdateComponentPort.Input(null, null, null, null, null, null);
        when(componentGateway.findById(9)).thenReturn(Optional.empty());

        assertThrows(InvalidUpdateComponentException.class, () -> useCase.execute(0, input));
        assertThrows(ComponentNotFoundException.class, () -> useCase.execute(9, input));
    }

    @Test
    @DisplayName("listing filters by category and name, by name only, or returns all")
    void list() {
        when(componentGateway.findByCategory("Elétrica")).thenReturn(List.of(part(1, "Capacitor"), part(2, "Fusível"), part(3, null)));
        when(componentGateway.findByName("fus")).thenReturn(List.of(part(2, "Fusível")));
        when(componentGateway.findAll()).thenReturn(List.of(part(1, "Capacitor")));
        GetComponentsImpl useCase = new GetComponentsImpl(componentGateway);

        assertEquals(List.of(2), useCase.execute("Elétrica", "FUS").stream().map(ComponentOutput::id).toList());
        assertEquals(3, useCase.execute("Elétrica", " ").size());
        assertEquals(List.of(2), useCase.execute(null, "fus").stream().map(ComponentOutput::id).toList());
        assertEquals(List.of(1), useCase.execute(" ", null).stream().map(ComponentOutput::id).toList());
    }

    @Test
    @DisplayName("get by id maps the component when it exists")
    void getById() {
        when(componentGateway.findById(1)).thenReturn(Optional.of(part(1, "Capacitor")));
        when(componentGateway.findById(2)).thenReturn(Optional.empty());
        GetComponentByIdImpl useCase = new GetComponentByIdImpl(componentGateway);

        assertEquals("Capacitor", useCase.execute(1).orElseThrow().item());
        assertTrue(useCase.execute(2).isEmpty());
    }

    @Test
    @DisplayName("delete removes components even with demands and validates the id")
    void delete() {
        ComponentPart withDemand = part(4, "Motor");
        withDemand.setDemands(new ArrayList<>(List.of(Demand.builder().id(1).order(Order.builder().id(9).build()).build())));
        when(componentGateway.findById(4)).thenReturn(Optional.of(withDemand));
        when(componentGateway.findById(5)).thenReturn(Optional.empty());
        DeleteComponentImpl useCase = new DeleteComponentImpl(componentGateway);

        useCase.execute(4);

        verify(componentGateway).deleteById(4);
        assertThrows(InvalidDeleteComponentException.class, () -> useCase.execute(null));
        assertThrows(ComponentNotFoundException.class, () -> useCase.execute(5));
    }
}
