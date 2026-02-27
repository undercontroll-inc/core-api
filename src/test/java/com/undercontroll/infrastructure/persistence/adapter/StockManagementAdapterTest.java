package com.undercontroll.infrastructure.persistence.adapter;

import com.undercontroll.domain.exception.ComponentNotFoundException;
import com.undercontroll.domain.exception.InsuficientComponentException;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.infrastructure.persistence.entity.ComponentPartJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.ComponentJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockManagementAdapterTest {

    @Mock
    private ComponentJpaRepository componentJpaRepository;

    @InjectMocks
    private StockManagementAdapter stockManagementAdapter;

    private ComponentPart component;
    private ComponentPartJpaEntity componentJpaEntity;

    @BeforeEach
    void setUp() {
        component = new ComponentPart();
        component.setId(1);
        component.setName("Resistor 10K");
        component.setQuantity(100L);

        componentJpaEntity = ComponentPartJpaEntity.fromDomain(component);
    }

    @Test
    @DisplayName("Deve diminuir o estoque quando houver quantidade suficiente")
    void testDecreaseStock_ShouldDecreaseSuccessfully() {
        when(componentJpaRepository.findById(1)).thenReturn(Optional.of(componentJpaEntity));
        when(componentJpaRepository.save(any(ComponentPartJpaEntity.class))).thenReturn(componentJpaEntity);

        stockManagementAdapter.decreaseStock(1, 30);

        ArgumentCaptor<ComponentPartJpaEntity> captor = ArgumentCaptor.forClass(ComponentPartJpaEntity.class);
        verify(componentJpaRepository, times(1)).save(captor.capture());
        assertEquals(70L, captor.getValue().getQuantity());
        verify(componentJpaRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar diminuir estoque insuficiente")
    void testDecreaseStock_ShouldThrowException_WhenInsufficientStock() {
        when(componentJpaRepository.findById(1)).thenReturn(Optional.of(componentJpaEntity));

        InsuficientComponentException exception = assertThrows(
                InsuficientComponentException.class,
                () -> stockManagementAdapter.decreaseStock(1, 150)
        );

        assertTrue(exception.getMessage().contains("Insufficient stock"));
        verify(componentJpaRepository, times(1)).findById(1);
        verify(componentJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve aumentar o estoque corretamente")
    void testIncreaseStock_ShouldIncreaseSuccessfully() {
        when(componentJpaRepository.findById(1)).thenReturn(Optional.of(componentJpaEntity));
        when(componentJpaRepository.save(any(ComponentPartJpaEntity.class))).thenReturn(componentJpaEntity);

        stockManagementAdapter.increaseStock(1, 50);

        ArgumentCaptor<ComponentPartJpaEntity> captor = ArgumentCaptor.forClass(ComponentPartJpaEntity.class);
        verify(componentJpaRepository, times(1)).save(captor.capture());
        assertEquals(150L, captor.getValue().getQuantity());
        verify(componentJpaRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Deve validar disponibilidade quando houver estoque suficiente")
    void testValidateStockAvailability_ShouldPass_WhenStockSufficient() {
        assertDoesNotThrow(() ->
                stockManagementAdapter.validateStockAvailability(component, 50)
        );
    }

    @Test
    @DisplayName("Deve lançar exceção quando estoque for insuficiente")
    void testValidateStockAvailability_ShouldThrowException_WhenStockInsufficient() {
        InsuficientComponentException exception = assertThrows(
                InsuficientComponentException.class,
                () -> stockManagementAdapter.validateStockAvailability(component, 150)
        );

        assertTrue(exception.getMessage().contains("Insufficient stock"));
        assertTrue(exception.getMessage().contains("Resistor 10K"));
    }

    @Test
    @DisplayName("Deve retornar componente quando existir")
    void testFindComponentById_ShouldReturnComponent_WhenExists() {
        when(componentJpaRepository.findById(1)).thenReturn(Optional.of(componentJpaEntity));

        Optional<ComponentPart> result = stockManagementAdapter.findComponentById(1);

        assertTrue(result.isPresent());
        assertEquals("Resistor 10K", result.get().getName());
        verify(componentJpaRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("Deve retornar Optional vazio quando componente não existir")
    void testFindComponentById_ShouldReturnEmpty_WhenNotExists() {
        when(componentJpaRepository.findById(999)).thenReturn(Optional.empty());

        Optional<ComponentPart> result = stockManagementAdapter.findComponentById(999);

        assertTrue(result.isEmpty());
        verify(componentJpaRepository, times(1)).findById(999);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar diminuir estoque de componente inexistente")
    void testDecreaseStock_ShouldThrowException_WhenComponentNotFound() {
        when(componentJpaRepository.findById(999)).thenReturn(Optional.empty());

        ComponentNotFoundException exception = assertThrows(ComponentNotFoundException.class,
                () -> stockManagementAdapter.decreaseStock(999, 10)
        );

        assertTrue(exception.getMessage().contains("Component not found"));
        verify(componentJpaRepository, times(1)).findById(999);
        verify(componentJpaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar aumentar estoque de componente inexistente")
    void testIncreaseStock_ShouldThrowException_WhenComponentNotFound() {
        when(componentJpaRepository.findById(999)).thenReturn(Optional.empty());

        ComponentNotFoundException exception = assertThrows(ComponentNotFoundException.class,
                () -> stockManagementAdapter.increaseStock(999, 10)
        );

        assertTrue(exception.getMessage().contains("Component not found"));
        verify(componentJpaRepository, times(1)).findById(999);
        verify(componentJpaRepository, never()).save(any());
    }
}
