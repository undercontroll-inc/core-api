package com.undercontroll.infrastructure.messaging;

import com.undercontroll.domain.enums.AnnouncementType;
import com.undercontroll.domain.event.AnnouncementCreatedEvent;
import com.undercontroll.domain.event.UserCreatedEvent;
import com.undercontroll.domain.model.Announcement;
import com.undercontroll.infrastructure.messaging.producers.NotificationProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificationProducerTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final NotificationProducer producer = new NotificationProducer(rabbitTemplate);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(producer, "notificationExchange", "notification.events");
        ReflectionTestUtils.setField(producer, "announcementRoutingKey", "announcement.created");
        ReflectionTestUtils.setField(producer, "userCreatedRoutingKey", "user.created");
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("publishes announcement events with the bearer token")
    void announcementCreated() {
        Announcement announcement = Announcement.builder()
                .id(3).title("Feriado").content("Fechado").type(AnnouncementType.HOLIDAY)
                .publishedAt(LocalDateTime.of(2026, 1, 10, 9, 0)).build();

        producer.handleAnnouncementCreated(new AnnouncementCreatedEvent(announcement, null));

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("notification.events"), eq("announcement.created"), payload.capture());
        Map<String, Object> body = (Map<String, Object>) payload.getValue();
        assertThat(body).containsEntry("type", "ANNOUNCEMENT_CREATED");
        assertThat((Map<String, Object>) body.get("data")).containsEntry("id", 3).containsEntry("token", "");
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("publishes user events and rethrows broker failures")
    void userCreated() {
        producer.handleUserCreated(new UserCreatedEvent(null, "ana@x.com", null));

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("notification.events"), eq("user.created"), payload.capture());
        assertThat((Map<String, Object>) ((Map<String, Object>) payload.getValue()).get("data")).containsEntry("email", "ana@x.com");

        doThrow(new IllegalStateException("down")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        assertThatThrownBy(() -> producer.handleUserCreated(new UserCreatedEvent("Ana", "a@x.com", LocalDateTime.now())))
                .isInstanceOf(IllegalStateException.class);
    }
}
