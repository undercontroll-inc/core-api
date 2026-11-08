package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.enums.OrderActor;
import com.undercontroll.domain.enums.OrderPhotoKind;
import com.undercontroll.domain.enums.OrderStatus;
import com.undercontroll.domain.exception.OrderConcurrentUpdateException;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderPhoto;
import com.undercontroll.domain.model.OrderStatusEvent;
import com.undercontroll.infrastructure.mapper.ComponentPartMapperImpl;
import com.undercontroll.infrastructure.mapper.DemandMapperImpl;
import com.undercontroll.infrastructure.mapper.OrderItemMapperImpl;
import com.undercontroll.infrastructure.mapper.OrderMapperImpl;
import com.undercontroll.infrastructure.mapper.UserMapperImpl;
import com.undercontroll.infrastructure.persistence.repository.OrderJpaRepository;
import com.undercontroll.infrastructure.persistence.repository.OrderPhotoJpaRepository;
import com.undercontroll.infrastructure.persistence.repository.OrderStatusEventJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = false)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
        OrderGatewayImpl.class,
        OrderStatusEventGatewayImpl.class,
        OrderPhotoGatewayImpl.class,
        OrderMapperImpl.class,
        OrderItemMapperImpl.class,
        DemandMapperImpl.class,
        UserMapperImpl.class,
        ComponentPartMapperImpl.class
})
class OrderLifecyclePersistenceTest {

    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");

    @Autowired
    private OrderGatewayImpl orders;

    @Autowired
    private OrderStatusEventGatewayImpl events;

    @Autowired
    private OrderPhotoGatewayImpl photos;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private OrderStatusEventJpaRepository eventRepository;

    @Autowired
    private OrderPhotoJpaRepository photoRepository;

    @AfterEach
    void cleanUp() {
        photoRepository.deleteAll();
        eventRepository.deleteAll();
        orderRepository.deleteAll();
    }

    private Order created() {
        return orders.save(Order.builder().status(OrderStatus.RECEIVED).discount(0.0).total(0.0).build());
    }

    @Test
    @DisplayName("the slower of two writers on the same version gets ORDER_CONCURRENT_UPDATE")
    void optimisticLock() {
        Order created = created();
        Order shop = orders.findById(created.getId()).orElseThrow();
        Order link = orders.findById(created.getId()).orElseThrow();

        shop.setStatus(OrderStatus.AWAITING_APPROVAL);
        orders.save(shop);
        link.setStatus(OrderStatus.READY_FOR_PICKUP);

        assertThrows(OrderConcurrentUpdateException.class, () -> orders.save(link));
        assertEquals(OrderStatus.AWAITING_APPROVAL, orders.findById(created.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("finds an order by its tracking token")
    void trackingToken() {
        Order created = created();
        created.setTrackingToken("7Hq2xLmP9vKa3TzR8dNw4c");
        orders.save(created);

        assertEquals(created.getId(), orders.findByTrackingToken("7Hq2xLmP9vKa3TzR8dNw4c").orElseThrow().getId());
        assertTrue(orders.findByTrackingToken("AAAAAAAAAAAAAAAAAAAAAA").isEmpty());
    }

    @Test
    @DisplayName("status history and photos are stored in order")
    void historyAndPhotos() {
        Integer id = created().getId();
        events.save(new OrderStatusEvent(null, id, OrderStatus.RECEIVED, OrderStatus.AWAITING_APPROVAL, null, OrderActor.SHOP, null, NOW));
        events.save(new OrderStatusEvent(null, id, OrderStatus.AWAITING_APPROVAL, OrderStatus.IN_REPAIR, null, OrderActor.CUSTOMER_LINK, null, NOW.plusSeconds(5)));
        OrderPhoto device = photos.save(new OrderPhoto(null, id, OrderPhotoKind.DEVICE, "order-photos/1/a.jpg", "image/jpeg", 10, NOW));
        photos.save(new OrderPhoto(null, id, OrderPhotoKind.LABEL, "order-photos/1/b.jpg", "image/jpeg", 10, NOW.plusSeconds(1)));

        List<OrderStatusEvent> history = events.findByOrderId(id);
        assertEquals(List.of(OrderStatus.AWAITING_APPROVAL, OrderStatus.IN_REPAIR), history.stream().map(OrderStatusEvent::to).toList());
        assertEquals(OrderActor.CUSTOMER_LINK, history.get(1).actor());
        assertEquals(2, photos.countByOrderId(id));
        assertEquals(device.id(), photos.findFirstByOrderIdAndKind(id, OrderPhotoKind.DEVICE).orElseThrow().id());
        assertTrue(photos.findById(id + 1, device.id()).isEmpty());
    }
}
