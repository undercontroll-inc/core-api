package com.undercontroll.infrastructure.messaging.consumer;

import com.undercontroll.domain.enums.EmailEventType;
import com.undercontroll.domain.events.AnnouncementCreatedEvent;
import com.undercontroll.domain.events.EmailEvent;
import com.undercontroll.domain.port.in.AnnouncementCreatedPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class EmailConsumer {

    private final AnnouncementCreatedPort announcementCreatedPort;

    @RabbitListener(queues = "notification.email.queue")
    public void listen(
            EmailEvent event
    ) {
        try {
            log.info("Received a event from {}, of type {}", event.service(), event.type());

            if(event.type().equals(EmailEventType.ANNOUNCEMENT_CREATED)) {
                announcementCreatedPort.execute(
                        (AnnouncementCreatedEvent) event.data()
                );
            }
        } catch (ClassCastException e) {
            log.error("Error while casting the event: {}", e.getMessage());
        }
    }

}
