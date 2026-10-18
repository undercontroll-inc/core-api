package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.gateway.AnnouncementGateway;
import com.undercontroll.domain.gateway.ComponentGateway;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.model.ComponentPart;
import com.undercontroll.domain.model.Demand;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderItem;
import com.undercontroll.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnaShopToolsTest {

    @Mock
    private OrderGateway orderGateway;

    @Mock
    private ComponentGateway componentGateway;

    @Mock
    private AnnouncementGateway announcementGateway;

    @InjectMocks
    private AnaShopTools tools;

    @Test
    @DisplayName("getOrder uses the detail graph and caps items and demands")
    void getOrderLimitsCollections() {
        List<OrderItem> items = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> OrderItem.builder().id(i).type("tipo").build())
                .toList();
        List<Demand> demands = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> Demand.builder().id(i).quantity(1L).build())
                .toList();
        when(orderGateway.findDetailById(9)).thenReturn(Optional.of(
                Order.builder()
                        .id(9)
                        .status(OrderStatus.PENDING)
                        .customerDescription("Não gela")
                        .technicalDescription("Compressor queimado")
                        .user(User.builder().name("João").lastName("Silva").build())
                        .orderItems(items)
                        .demands(demands)
                        .build()
        ));

        AnaShopTools.OrderDetail detail = tools.getOrder(9);

        assertEquals(9, detail.order().id());
        assertEquals("João Silva", detail.order().customer());
        assertEquals("Não gela", detail.order().customerDescription());
        assertEquals("Compressor queimado", detail.order().technicalDescription());
        assertEquals(AnaShopTools.LIMIT, detail.items().size());
        assertEquals(AnaShopTools.LIMIT, detail.demands().size());
        verify(orderGateway, never()).findAll();
        verify(orderGateway, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("searchComponents without filter lists lowest stock")
    void searchComponentsLowestStock() {
        when(componentGateway.findLowestStock(AnaShopTools.LIMIT)).thenReturn(List.of(
                ComponentPart.builder().id(1).name("Resistência").quantity(2L).build()
        ));

        List<AnaShopTools.PartLine> parts = tools.searchComponents(null, null);

        assertEquals(1, parts.size());
        verify(componentGateway, never()).findAll(anyInt());
    }

    @Test
    @DisplayName("searchComponents uses paginated name lookup")
    void searchComponentsByName() {
        when(componentGateway.searchByName("resistência", AnaShopTools.LIMIT)).thenReturn(List.of(
                ComponentPart.builder().id(1).name("Resistência").quantity(4L).build()
        ));

        List<AnaShopTools.PartLine> parts = tools.searchComponents("resistência", null);

        assertEquals(1, parts.size());
        assertEquals(4L, parts.get(0).quantity());
        verify(componentGateway, never()).findByName(anyString(), anyInt());
        verify(componentGateway, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("searchComponents uses paginated category lookup")
    void searchComponentsByCategory() {
        when(componentGateway.searchByCategory("resistencias", AnaShopTools.LIMIT)).thenReturn(List.of(
                ComponentPart.builder().id(2).name("Resistência 220").quantity(1L).build()
        ));

        List<AnaShopTools.PartLine> parts = tools.searchComponents(null, "resistencias");

        assertEquals(1, parts.size());
        verify(componentGateway, never()).findByCategory(anyString(), anyInt());
    }

    @Test
    @DisplayName("getOrder returns null when missing")
    void missingOrder() {
        when(orderGateway.findDetailById(99)).thenReturn(Optional.empty());
        assertNull(tools.getOrder(99));
    }

    @Test
    @DisplayName("getOrder returns null when the gateway throws")
    void getOrderSwallowsGatewayFailure() {
        when(orderGateway.findDetailById(9)).thenThrow(new IllegalStateException("cannot simultaneously fetch multiple bags"));
        assertNull(tools.getOrder(9));
    }
}
